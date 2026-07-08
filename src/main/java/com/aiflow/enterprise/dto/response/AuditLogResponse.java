package com.aiflow.enterprise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {

    private String id;
    private String action;
    private String entityType;
    private String entityId;
    private String performedBy;
    private Map<String, Object> previousValues;
    private Map<String, Object> newValues;
    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
    private String browser;
    private String device;
    private String deviceOs;
    private String correlationId;
    private String sessionId;
    private String requestId;
    private String workflowId;
    private String executionId;
    private String endpoint;
    private String httpMethod;
    private String organizationId;
    private boolean success;
    private boolean immutable;
    private String failureReason;
    private long durationMs;
    private Instant timestamp;
    private Instant createdAt;
}
