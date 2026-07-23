package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.engine.core.webhook.WebhookAuthProvider;
import com.aiflow.enterprise.engine.core.webhook.WebhookCircuitBreakerManager;
import com.aiflow.enterprise.engine.core.webhook.WebhookResponseValidator;
import com.aiflow.enterprise.entity.WebhookExecutionHistory;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import com.aiflow.enterprise.repository.WebhookExecutionHistoryRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WebhookStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookStepProcessor.class);

    private static final List<String> SENSITIVE_HEADERS = List.of("authorization", "x-api-key", "api-key", "token");

    private final WebClient.Builder webClientBuilder;
    private final WebhookExecutionHistoryRepository historyRepository;
    private final WebhookCircuitBreakerManager circuitBreakerManager;

    @Value("${app.webhook.default-connect-timeout-ms:10000}")
    private int defaultConnectTimeoutMs;

    @Value("${app.webhook.default-read-timeout-ms:30000}")
    private int defaultReadTimeoutMs;

    @Value("${app.webhook.max-payload-size-bytes:1048576}")
    private int maxPayloadSizeBytes;

    @Value("${app.webhook.user-agent:AIFlow-Enterprise-Webhook/1.0}")
    private String userAgent;

    public WebhookStepProcessor(WebClient.Builder webClientBuilder,
                                WebhookExecutionHistoryRepository historyRepository,
                                WebhookCircuitBreakerManager circuitBreakerManager) {
        this.webClientBuilder = webClientBuilder;
        this.historyRepository = historyRepository;
        this.circuitBreakerManager = circuitBreakerManager;
    }

    @Override
    public StepType getType() {
        return StepType.WEBHOOK;
    }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig() != null ? step.getConfig() : Map.of();
        String correlationId = UUID.randomUUID().toString();

        try {
            return executeWebhook(step, ctx, config, correlationId);
        } catch (Exception e) {
            log.error("Webhook execution error for step {}: {}", step.getStepId(), e.getMessage(), e);
            return StepResult.failure("Webhook execution failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private StepResult executeWebhook(WorkflowStep step, ExecutionContext ctx,
                                      Map<String, Object> config, String correlationId) {
        String rawUrl = getConfig(config, "url", "");
        String url = resolveTemplate(rawUrl, ctx);
        if (url.isBlank()) {
            return StepResult.failure("Webhook URL is not configured");
        }

        String method = getConfig(config, "method", "POST").toUpperCase();
        String contentType = getConfig(config, "contentType", "application/json");
        int connectTimeout = parseIntConfig(config, "connectionTimeout", defaultConnectTimeoutMs / 1000);
        int readTimeout = parseIntConfig(config, "timeout", defaultReadTimeoutMs / 1000);

        Map<String, Object> authConfig = getConfigTyped(config, "auth", Map.class);
        Map<String, Object> successConditions = getConfigTyped(config, "successConditions", Map.class);
        Map<String, Object> cbConfig = getConfigTyped(config, "circuitBreaker", Map.class);
        Map<String, Object> sslConfig = getConfigTyped(config, "ssl", Map.class);

        boolean validateSsl = sslConfig != null
                ? parseBoolConfig(sslConfig, "validateCertificates", true)
                : true;

        WebhookAuthProvider authProvider = WebhookAuthProvider.forConfig(authConfig);
        WebhookResponseValidator validator = new WebhookResponseValidator(successConditions);

        Map<String, String> headers = buildHeaders(config, contentType);
        authProvider.enrichHeaders(authConfig).accept(headers);

        String body = resolveTemplate(getConfig(config, "body", ""), ctx);
        Map<String, String> queryParams = resolveParamMap(getConfigTyped(config, "queryParams", Map.class), ctx);
        Map<String, String> pathVariables = resolveParamMap(getConfigTyped(config, "pathVariables", Map.class), ctx);

        String resolvedUrl = applyPathVariables(url, pathVariables);

        URI requestUri = buildUri(resolvedUrl, queryParams);

        WebClient client = buildWebClient(connectTimeout, readTimeout, validateSsl, headers);

        CircuitBreaker circuitBreaker = circuitBreakerManager.getOrCreate(resolvedUrl, cbConfig);

        Instant start = Instant.now();
        AtomicInteger retryAttempt = new AtomicInteger(0);
        int totalRetries = step.getRetryConfig() != null ? step.getRetryConfig().getMaxAttempts() : 0;
        String lastError = null;
        int statusCode = 0;
        String responseBody = null;
        Map<String, String> responseHeaders = null;
        boolean success = false;

        try {
            WebhookResult result = circuitBreaker.executeSupplier(() -> {
                int attempt = 0;
                String err = null;

                for (int i = 0; i < Math.max(totalRetries, 1); i++) {
                    attempt++;
                    Instant callStart = Instant.now();
                    retryAttempt.set(attempt);

                    log.info("Webhook [{}] {} (attempt {}/{}, correlationId={})",
                            method, requestUri, attempt, Math.max(totalRetries, 1), correlationId);

                    try {
                        WebhookResponse response = doHttpCall(client, method, body, contentType, requestUri);
                        long duration = Duration.between(callStart, Instant.now()).toMillis();

                        WebhookResponseValidator.ValidationResult vr = validator.validate(
                                response.statusCode, response.body);

                        if (vr.isSuccess()) {
                            return new WebhookResult(
                                    response.statusCode, response.body, response.headers,
                                    true, duration, null);
                        }

                        err = vr.getMessage();
                        log.warn("Webhook validation failed for {}: {} (correlationId={})",
                                requestUri, err, correlationId);

                        if (i < Math.max(totalRetries, 1) - 1) {
                            long backoffMs = calculateBackoff(i, config);
                            log.info("Retrying webhook {} in {}ms (attempt {}/{})",
                                    requestUri, backoffMs, attempt, Math.max(totalRetries, 1));
                            try {
                                Thread.sleep(backoffMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    } catch (WebhookExecutionException e) {
                        err = e.getMessage();
                        log.error("Webhook call failed: {} (correlationId={})", err, correlationId);
                        if (i < Math.max(totalRetries, 1) - 1) {
                            long backoffMs = calculateBackoff(i, config);
                            log.info("Retrying webhook {} in {}ms (attempt {}/{})",
                                    requestUri, backoffMs, attempt, Math.max(totalRetries, 1));
                            try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                        }
                    }
                }

                return new WebhookResult(0, null, null, false,
                        Duration.between(start, Instant.now()).toMillis(),
                        err != null ? err : "Webhook failed after " + attempt + " attempts");
            });

            statusCode = result.statusCode;
            responseBody = result.body;
            responseHeaders = result.headers;
            success = result.success;
            lastError = result.error;

            if (lastError == null && !success) {
                lastError = "Webhook response validation failed";
            }

        } catch (CallNotPermittedException e) {
            lastError = "Circuit breaker is OPEN for " + resolvedUrl;
            log.warn("{} (correlationId={})", lastError, correlationId);
            success = false;
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Webhook execution exception for {}: {} (correlationId={})",
                    resolvedUrl, lastError, correlationId);
            success = false;
        }

        long executionTimeMs = Duration.between(start, Instant.now()).toMillis();

        saveHistory(resolvedUrl, step, ctx, config, correlationId, statusCode, executionTimeMs,
                success, lastError, responseHeaders, sanitizeHeaders(headers), retryAttempt.get(),
                totalRetries, responseBody, authProvider.getType());

        if (success) {
            Map<String, Object> output = new HashMap<>();
            output.put("url", resolvedUrl);
            output.put("method", method);
            output.put("statusCode", statusCode);
            output.put("responseBody", truncateBody(responseBody));
            output.put("responseHeaders", responseHeaders != null ? responseHeaders : Map.of());
            output.put("executionTimeMs", executionTimeMs);
            output.put("correlationId", correlationId);
            output.put("retryAttempts", retryAttempt.get());
            output.put("circuitBreakerState", circuitBreaker.getState().name());

            Map<String, Object> data = new HashMap<>();
            data.put("webhook_response_" + step.getStepId(), responseBody);
            data.put("webhook_status_" + step.getStepId(), statusCode);
            data.put("webhook_correlation_id_" + step.getStepId(), correlationId);

            return StepResult.success(output, data);
        }

        return StepResult.failure(lastError != null ? lastError : "Webhook execution failed");
    }

    private WebhookResponse doHttpCall(WebClient client, String method, String body,
                                        String contentType, URI uri) {
        try {
            WebClient.RequestBodySpec spec = client.method(HttpMethod.valueOf(method)).uri(uri);

            boolean hasBody = !"GET".equalsIgnoreCase(method)
                    && !"DELETE".equalsIgnoreCase(method)
                    && body != null && !body.isEmpty();

            if (hasBody) {
                MediaType mediaType = parseMediaType(contentType);
                if (MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType)) {
                    spec.body(BodyInserters.fromFormData(parseFormData(body)));
                } else {
                    spec.body(BodyInserters.fromValue(body));
                }
            }

            return spec.exchangeToMono(response -> {
                        HttpHeaders respHeaders = response.headers().asHttpHeaders();
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(responseBody -> new WebhookResponse(
                                        response.statusCode().value(),
                                        responseBody,
                                        toStringMap(respHeaders)
                                ));
                    })
                    .block();
        } catch (WebClientResponseException e) {
            Map<String, String> respHeaders = new HashMap<>();
            e.getHeaders().forEach((k, vs) ->
                    respHeaders.put(k, String.join(", ", vs)));
            return new WebhookResponse(
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString(),
                    respHeaders
            );
        } catch (Exception e) {
            throw new WebhookExecutionException(
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private WebClient buildWebClient(int connectTimeoutSec, int readTimeoutSec,
                                      boolean validateSsl, Map<String, String> headers) {
        HttpClient httpClient = HttpClient.create()
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutSec * 1000)
                .responseTimeout(Duration.ofSeconds(readTimeoutSec));

        if (!validateSsl) {
            httpClient = httpClient.secure(t -> {
                try {
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ctx.init(null, new TrustManager[]{
                            new X509TrustManager() {
                                public void checkClientTrusted(java.security.cert.X509Certificate[] c, String a) {}
                                public void checkServerTrusted(java.security.cert.X509Certificate[] c, String a) {}
                                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
                            }
                    }, new SecureRandom());
                    t.sslContext(SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build());
                } catch (Exception e) {
                    throw new RuntimeException("Failed to configure SSL", e);
                }
            });
        }

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent);

        if (headers != null) {
            headers.forEach((key, value) -> {
                if (!HttpHeaders.USER_AGENT.equalsIgnoreCase(key)
                        && !HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(key)
                        && !HttpHeaders.ACCEPT.equalsIgnoreCase(key)
                        && !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(key)
                        && !key.toLowerCase().startsWith("x-api")) {
                    builder.defaultHeader(key, value);
                }
            });
        }

        return builder.build();
    }

    private URI buildUri(String url, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return URI.create(url);
        }
        StringBuilder sb = new StringBuilder(url);
        boolean hasQuery = url.contains("?");
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            sb.append(hasQuery ? '&' : '?');
            hasQuery = true;
            sb.append(java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return URI.create(sb.toString());
    }

    private long calculateBackoff(int attempt, Map<String, Object> config) {
        long baseDelay = 1000L;
        double multiplier = 2.0;
        if (config != null) {
            if (config.containsKey("retryDelayMs")) {
                baseDelay = Long.parseLong(config.get("retryDelayMs").toString());
            }
            if (config.containsKey("retryBackoffMultiplier")) {
                multiplier = Double.parseDouble(config.get("retryBackoffMultiplier").toString());
            }
        }
        return (long) (baseDelay * Math.pow(multiplier, attempt));
    }

    private void saveHistory(String url, WorkflowStep step, ExecutionContext ctx,
                              Map<String, Object> config, String correlationId,
                              int statusCode, long executionTimeMs, boolean success,
                              String failureReason, Map<String, String> responseHeaders,
                              Map<String, String> safeRequestHeaders,
                              int retryAttempt, int totalRetries,
                              String responseBody, String authType) {
        try {
            String safeUrl = url != null ? url : getConfig(config, "url", "");
            String safeResponseBody = truncateBody(responseBody);

            WebhookExecutionHistory history = WebhookExecutionHistory.builder()
                    .executionId(ctx.getExecutionId())
                    .workflowId(ctx.getWorkflowId())
                    .stepId(step.getStepId())
                    .stepName(step.getName())
                    .url(safeUrl)
                    .method(getConfig(config, "method", "POST").toUpperCase())
                    .requestHeaders(safeRequestHeaders)
                    .requestBody(truncateBody(getConfig(config, "body", "")))
                    .responseHeaders(responseHeaders)
                    .responseBody(safeResponseBody)
                    .statusCode(statusCode)
                    .success(success)
                    .executionTimeMs(executionTimeMs)
                    .retryAttempt(retryAttempt)
                    .totalRetries(totalRetries)
                    .failureReason(failureReason)
                    .correlationId(correlationId)
                    .authType(authType != null ? authType : "none")
                    .sslVerified(parseBoolConfig(config, "ssl.validateCertificates", true))
                    .circuitBreakerState(circuitBreakerManager.getState(safeUrl).name())
                    .startedAt(Instant.now().minusMillis(executionTimeMs))
                    .completedAt(Instant.now())
                    .build();

            historyRepository.save(history);
            log.debug("Saved webhook execution history for step {} (success={})", step.getStepId(), success);
        } catch (Exception e) {
            log.error("Failed to save webhook execution history: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildHeaders(Map<String, Object> config, String contentType) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, contentType);
        headers.put(HttpHeaders.ACCEPT, "application/json, */*");

        Object headersRaw = config.get("headers");
        if (headersRaw instanceof Map) {
            ((Map<String, Object>) headersRaw).forEach((key, value) -> {
                if (value != null) headers.put(key, value.toString());
            });
        }

        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> resolveParamMap(Map<String, Object> rawParams, ExecutionContext ctx) {
        if (rawParams == null) return Map.of();
        Map<String, String> resolved = new HashMap<>();
        rawParams.forEach((key, value) -> {
            String val = value != null ? value.toString() : "";
            resolved.put(key, resolveTemplate(val, ctx));
        });
        return resolved;
    }

    private String applyPathVariables(String url, Map<String, String> pathVariables) {
        if (pathVariables == null || pathVariables.isEmpty()) return url;
        String result = url;
        for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String encoded = java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            result = result.replace(placeholder, encoded);
            result = result.replace("{" + entry.getKey() + "}", encoded);
        }
        return result;
    }

    private Map<String, String> toStringMap(HttpHeaders headers) {
        if (headers == null) return Map.of();
        Map<String, String> result = new HashMap<>();
        headers.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, String.join(", ", values));
            }
        });
        return result;
    }

    private Map<String, String> sanitizeHeaders(Map<String, String> headers) {
        if (headers == null) return Map.of();
        Map<String, String> safe = new HashMap<>(headers);
        for (String key : safe.keySet()) {
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                safe.put(key, "****");
            }
        }
        return safe;
    }

    private MediaType parseMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_JSON;
        }
    }

    private MultiValueMap<String, String> parseFormData(String body) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        if (body == null || body.isEmpty()) return formData;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                formData.add(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return formData;
    }

    private int parseIntConfig(Map<String, Object> config, String key, int defaultValue) {
        Object val = config != null ? config.get(key) : null;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private boolean parseBoolConfig(Map<String, Object> config, String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        Object val = config.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return defaultValue;
    }

    private String truncateBody(String body) {
        if (body == null) return null;
        if (body.length() > maxPayloadSizeBytes) {
            return body.substring(0, maxPayloadSizeBytes) + "... [truncated]";
        }
        return body;
    }

    private static class WebhookExecutionException extends RuntimeException {
        WebhookExecutionException(String message) {
            super(message);
        }
    }

    private static class WebhookResponse {
        final int statusCode;
        final String body;
        final Map<String, String> headers;

        WebhookResponse(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
    }

    private static class WebhookResult {
        final int statusCode;
        final String body;
        final Map<String, String> headers;
        final boolean success;
        final long durationMs;
        final String error;

        WebhookResult(int statusCode, String body, Map<String, String> headers,
                      boolean success, long durationMs, String error) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
            this.success = success;
            this.durationMs = durationMs;
            this.error = error;
        }
    }
}
