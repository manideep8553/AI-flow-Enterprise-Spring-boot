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
    private Map<String, Object> details;
    private String ipAddress;
    private Instant timestamp;
    private Instant createdAt;
}
