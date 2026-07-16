package com.aiflow.enterprise.engine.core.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class WebhookResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookResponseValidator.class);

    private final List<Integer> expectedStatusCodes;
    private final List<String> bodyContains;
    private final boolean validateBody;

    @SuppressWarnings("unchecked")
    public WebhookResponseValidator(Map<String, Object> successConditions) {
        if (successConditions == null) {
            this.expectedStatusCodes = List.of(200, 201, 202, 204);
            this.bodyContains = List.of();
            this.validateBody = false;
        } else {
            Object statusCodesRaw = successConditions.get("statusCodes");
            if (statusCodesRaw instanceof List) {
                this.expectedStatusCodes = ((List<Object>) statusCodesRaw).stream()
                        .map(v -> v instanceof Number ? ((Number) v).intValue() : Integer.parseInt(v.toString()))
                        .toList();
            } else {
                this.expectedStatusCodes = List.of(200, 201, 202, 204);
            }

            Object bodyContainsRaw = successConditions.get("bodyContains");
            if (bodyContainsRaw instanceof List) {
                this.bodyContains = ((List<Object>) bodyContainsRaw).stream()
                        .map(Object::toString)
                        .toList();
            } else {
                this.bodyContains = List.of();
            }

            this.validateBody = !bodyContains.isEmpty();
        }
    }

    public ValidationResult validate(int statusCode, String responseBody) {
        if (!expectedStatusCodes.contains(statusCode)) {
            String msg = String.format("Status code %d not in expected codes %s", statusCode, expectedStatusCodes);
            log.warn(msg);
            return ValidationResult.failure(msg);
        }

        if (validateBody && responseBody != null) {
            for (String contain : bodyContains) {
                if (!responseBody.contains(contain)) {
                    String msg = String.format("Response body does not contain expected content: '%s'", contain);
                    log.warn(msg);
                    return ValidationResult.failure(msg);
                }
            }
        }

        return ValidationResult.success();
    }

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
