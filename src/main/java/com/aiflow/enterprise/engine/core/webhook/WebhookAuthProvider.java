package com.aiflow.enterprise.engine.core.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

public interface WebhookAuthProvider {

    String getType();

    Consumer<Map<String, String>> enrichHeaders(Map<String, Object> authConfig);

    static WebhookAuthProvider forConfig(Map<String, Object> authConfig) {
        if (authConfig == null || authConfig.isEmpty()) return new NoopAuthProvider();
        String type = authConfig.containsKey("type") ? authConfig.get("type").toString().toLowerCase() : "";
        return switch (type) {
            case "bearer" -> new BearerTokenAuthProvider(authConfig);
            case "basic" -> new BasicAuthProvider(authConfig);
            case "api-key" -> new ApiKeyAuthProvider(authConfig);
            default -> new NoopAuthProvider();
        };
    }

    class NoopAuthProvider implements WebhookAuthProvider {
        @Override
        public String getType() { return "none"; }

        @Override
        public Consumer<Map<String, String>> enrichHeaders(Map<String, Object> authConfig) {
            return headers -> {};
        }
    }

    class BearerTokenAuthProvider implements WebhookAuthProvider {
        private static final Logger log = LoggerFactory.getLogger(BearerTokenAuthProvider.class);
        private final String token;

        public BearerTokenAuthProvider(Map<String, Object> config) {
            this.token = config.getOrDefault("bearerToken", "").toString();
        }

        @Override
        public String getType() { return "bearer"; }

        @Override
        public Consumer<Map<String, String>> enrichHeaders(Map<String, Object> authConfig) {
            return headers -> {
                if (token != null && !token.isEmpty()) {
                    headers.put("Authorization", "Bearer " + token);
                    log.debug("Added Bearer token authorization header");
                }
            };
        }
    }

    class BasicAuthProvider implements WebhookAuthProvider {
        private static final Logger log = LoggerFactory.getLogger(BasicAuthProvider.class);
        private final String username;
        private final String password;

        public BasicAuthProvider(Map<String, Object> config) {
            this.username = config.getOrDefault("username", "").toString();
            this.password = config.getOrDefault("password", "").toString();
        }

        @Override
        public String getType() { return "basic"; }

        @Override
        public Consumer<Map<String, String>> enrichHeaders(Map<String, Object> authConfig) {
            return headers -> {
                if (!username.isEmpty()) {
                    String credentials = username + ":" + password;
                    String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                    headers.put("Authorization", "Basic " + encoded);
                    log.debug("Added Basic authorization header for user: {}", username);
                }
            };
        }
    }

    class ApiKeyAuthProvider implements WebhookAuthProvider {
        private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthProvider.class);
        private final String headerName;
        private final String apiKey;

        public ApiKeyAuthProvider(Map<String, Object> config) {
            this.headerName = config.getOrDefault("apiKeyHeader", "X-API-Key").toString();
            this.apiKey = config.getOrDefault("apiKeyValue", "").toString();
        }

        @Override
        public String getType() { return "api-key"; }

        @Override
        public Consumer<Map<String, String>> enrichHeaders(Map<String, Object> authConfig) {
            return headers -> {
                if (!apiKey.isEmpty() && !headerName.isEmpty()) {
                    headers.put(headerName, apiKey);
                    log.debug("Added API key header: {}", headerName);
                }
            };
        }
    }
}
