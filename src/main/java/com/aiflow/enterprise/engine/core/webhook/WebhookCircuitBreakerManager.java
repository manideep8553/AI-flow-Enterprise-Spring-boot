package com.aiflow.enterprise.engine.core.webhook;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebhookCircuitBreakerManager {

    private static final Logger log = LoggerFactory.getLogger(WebhookCircuitBreakerManager.class);

    private final CircuitBreakerRegistry registry;
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    @Value("${app.webhook.circuit-breaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${app.webhook.circuit-breaker.wait-duration-in-open-state:30s}")
    private String waitDurationInOpenState;

    @Value("${app.webhook.circuit-breaker.sliding-window-size:10}")
    private int slidingWindowSize;

    @Value("${app.webhook.circuit-breaker.minimum-number-of-calls:5}")
    private int minimumNumberOfCalls;

    @Value("${app.webhook.circuit-breaker.permitted-number-of-calls-in-half-open-state:3}")
    private int permittedNumberOfCallsInHalfOpenState;

    public WebhookCircuitBreakerManager() {
        this.registry = CircuitBreakerRegistry.ofDefaults();
    }

    @PostConstruct
    public void init() {
        String waitDurationStr = "PT" + waitDurationInOpenState.toUpperCase();
        if (!waitDurationStr.endsWith("S")) waitDurationStr += "S";
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.parse(waitDurationStr))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                .build();
        registry.addConfiguration("webhook-default", defaultConfig);
        log.info("Webhook circuit breaker initialized with config: failureRate={}%, window={}, minCalls={}",
                failureRateThreshold, slidingWindowSize, minimumNumberOfCalls);
    }

    public CircuitBreaker getOrCreate(String url, Map<String, Object> cbConfig) {
        String key = normalizeUrl(url);
        return breakers.computeIfAbsent(key, k -> {
            CircuitBreakerConfig config;
            if (cbConfig != null && !cbConfig.isEmpty()) {
                config = buildConfigFromMap(cbConfig);
            } else {
                config = registry.getConfiguration("webhook-default")
                        .orElse(CircuitBreakerConfig.ofDefaults());
            }
            CircuitBreaker breaker = registry.circuitBreaker(k, config);
            breaker.getEventPublisher()
                    .onStateTransition(event -> {
                        String from = event.getStateTransition().getFromState().name();
                        String to = event.getStateTransition().getToState().name();
                        log.warn("Circuit breaker [{}] state changed: {} -> {}", k, from, to);
                    });
            log.debug("Created circuit breaker for webhook endpoint: {}", k);
            return breaker;
        });
    }

    public CircuitBreaker.State getState(String url) {
        String key = normalizeUrl(url);
        CircuitBreaker breaker = breakers.get(key);
        return breaker != null ? breaker.getState() : CircuitBreaker.State.CLOSED;
    }

    private String normalizeUrl(String url) {
        return url.replaceAll("https?://", "")
                .replaceAll("[?&].*", "")
                .replaceAll("\\{\\{.*?\\}\\}", "{var}");
    }

    private CircuitBreakerConfig buildConfigFromMap(Map<String, Object> config) {
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();
        if (config.containsKey("failureThreshold")) {
            builder.failureRateThreshold(Float.parseFloat(config.get("failureThreshold").toString()));
        }
        if (config.containsKey("timeoutSeconds")) {
            builder.waitDurationInOpenState(Duration.ofSeconds(Long.parseLong(config.get("timeoutSeconds").toString())));
        }
        if (config.containsKey("slidingWindowSize")) {
            builder.slidingWindowSize(Integer.parseInt(config.get("slidingWindowSize").toString()));
        }
        if (config.containsKey("minimumNumberOfCalls")) {
            builder.minimumNumberOfCalls(Integer.parseInt(config.get("minimumNumberOfCalls").toString()));
        }
        if (config.containsKey("halfOpenMaxCalls")) {
            builder.permittedNumberOfCallsInHalfOpenState(Integer.parseInt(config.get("halfOpenMaxCalls").toString()));
        }
        return builder.build();
    }

    public void resetAll() {
        breakers.clear();
        log.info("All webhook circuit breakers reset");
    }
}
