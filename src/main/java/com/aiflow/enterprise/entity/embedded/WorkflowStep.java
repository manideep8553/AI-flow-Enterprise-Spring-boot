package com.aiflow.enterprise.entity.embedded;

import com.aiflow.enterprise.enums.StepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStep {
    private String stepId;
    private String name;
    private String description;
    private StepType type;
    private Integer order;
    private Map<String, Object> config;
    private List<String> dependsOn;
    private List<String> nextOnSuccess;
    private List<String> nextOnFailure;
    private Integer timeoutSeconds;
    private Boolean mandatory;
    private RetryConfig retryConfig;
    private LoopConfig loopConfig;
    private String errorStepId;
    private String completionCondition;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetryConfig {
        private int maxAttempts;
        private long delaySeconds;
        private double backoffMultiplier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoopConfig {
        private String type;
        private String collectionExpression;
        private String conditionExpression;
        private Integer maxIterations;
        private String loopVariable;
    }
}
