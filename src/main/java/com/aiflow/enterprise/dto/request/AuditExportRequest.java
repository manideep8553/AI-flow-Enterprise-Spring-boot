package com.aiflow.enterprise.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class AuditExportRequest {

    private String format;

    @Builder.Default
    private List<String> actions = List.of();

    private String entityType;
    private String entityId;
    private String performedBy;
    private String correlationId;
    private String workflowId;
    private String executionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant from;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant to;

    private Boolean success;
    private String organizationId;

    @Builder.Default
    private boolean includeMetadata = true;
}
