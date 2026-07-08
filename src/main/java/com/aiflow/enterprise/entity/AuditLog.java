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
@Document(collection = "audit_logs")
@CompoundIndex(def = "{'correlationId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'entityType': 1, 'entityId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'performedBy': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'action': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'workflowId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'executionId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'requestId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'sessionId': 1, 'timestamp': -1}")
public class AuditLog extends BaseEntity {

    @Indexed
    private String action;

    @Indexed
    private String entityType;

    @Indexed
    private String entityId;

    @Indexed
    private String performedBy;

    private Map<String, Object> previousValues;

    private Map<String, Object> newValues;

    private Map<String, Object> details;

    @Indexed
    private String ipAddress;

    private String userAgent;

    private String browser;

    private String device;

    private String deviceOs;

    @Indexed
    private String correlationId;

    @Indexed
    private String sessionId;

    @Indexed
    private String requestId;

    @Indexed
    private String workflowId;

    @Indexed
    private String executionId;

    private String endpoint;

    private String httpMethod;

    private String organizationId;

    private boolean success;

    private boolean immutable;

    private String failureReason;

    private long durationMs;

    @Indexed
    private Instant timestamp;
}
