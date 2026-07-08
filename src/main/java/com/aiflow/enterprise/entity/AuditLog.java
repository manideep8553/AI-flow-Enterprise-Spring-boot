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
@Document(collection = "audit_logs")
public class AuditLog extends BaseEntity {

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    @Indexed
    private String performedBy;

    private Map<String, Object> details;

    private String ipAddress;

    @Indexed
    private Instant timestamp;
}
