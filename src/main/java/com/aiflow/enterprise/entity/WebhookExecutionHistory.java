package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "webhook_execution_history")
@CompoundIndex(name = "webhook_lookup", def = "{'executionId':1, 'stepId':1, 'startedAt':-1}")
@CompoundIndex(name = "webhook_url_idx", def = "{'url':1, 'startedAt':-1}")
public class WebhookExecutionHistory extends BaseEntity {

    @Indexed
    private String executionId;

    @Indexed
    private String workflowId;

    private String stepId;

    private String stepName;

    @Indexed
    private String url;

    private String method;

    private Map<String, String> requestHeaders;

    private String requestBody;

    private Map<String, String> responseHeaders;

    private String responseBody;

    private int statusCode;

    private boolean success;

    private long executionTimeMs;

    private int retryAttempt;

    private int totalRetries;

    private String failureReason;

    @Indexed
    private String correlationId;

    private String authType;

    private boolean sslVerified;

    private String circuitBreakerState;

    private Instant startedAt;

    private Instant completedAt;
}
