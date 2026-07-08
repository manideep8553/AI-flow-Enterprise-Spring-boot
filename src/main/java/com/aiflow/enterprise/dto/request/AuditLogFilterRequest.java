package com.aiflow.enterprise.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogFilterRequest {

    private String action;
    private List<String> actions;
    private String entityType;
    private String entityId;
    private String performedBy;
    private String correlationId;
    private String sessionId;
    private String requestId;
    private String workflowId;
    private String executionId;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant from;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant to;

    private Boolean success;
    private Boolean immutable;
    private String searchTerm;
    private String organizationId;
    private String endpoint;
    private String httpMethod;
    private String sortBy;
    private String sortDirection;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
