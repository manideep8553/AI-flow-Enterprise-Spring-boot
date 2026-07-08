package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
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
@Document(collection = "compliance_reports")
public class ComplianceReport extends BaseEntity {

    @Indexed
    private String reportType;

    private String title;

    private String description;

    private Map<String, Object> parameters;

    private Map<String, Object> summary;

    @Indexed
    private String status;

    private String format;

    private long recordCount;

    @Indexed
    private String generatedBy;

    private Instant from;

    private Instant to;

    @Indexed
    private Instant generatedAt;

    private String downloadUrl;

    private String errorMessage;
}
