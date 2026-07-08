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
public class ComplianceReportResponse {

    private String id;
    private String reportType;
    private String title;
    private String description;
    private Map<String, Object> parameters;
    private Map<String, Object> summary;
    private String status;
    private String format;
    private long recordCount;
    private String generatedBy;
    private Instant from;
    private Instant to;
    private Instant generatedAt;
    private Instant createdAt;
    private String downloadUrl;
}
