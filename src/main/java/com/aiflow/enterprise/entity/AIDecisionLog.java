package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.ai.AIRequestType;
import com.aiflow.enterprise.ai.ConfidenceLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "ai_decision_logs")
public class AIDecisionLog extends BaseEntity {

    @Indexed
    private AIRequestType requestType;

    private String modelUsed;

    private int promptTokens;

    private int completionTokens;

    private int totalTokens;

    private long responseTimeMs;

    @Indexed
    private String requestId;

    private String requestTitle;

    @Indexed
    private String workflowId;

    @Indexed
    private String executionId;

    @Indexed
    private String userId;

    private String userQuery;

    private String aiResponse;

    private String recommendation;

    private ConfidenceLevel confidenceLevel;

    private double confidenceScore;

    private String reasoning;

    private boolean feedbackProvided;

    private boolean feedbackPositive;

    @Builder.Default
    private boolean accepted = false;

    private Instant createdAt;
}
