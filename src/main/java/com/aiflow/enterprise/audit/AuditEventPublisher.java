package com.aiflow.enterprise.audit;

import com.aiflow.enterprise.entity.AuditLog;
import com.aiflow.enterprise.enums.AuditActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditEventPublisher.class);

    private final AuditLogRepositoryBridge auditLogRepository;

    public AuditEventPublisher(AuditLogRepositoryBridge auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void publish(AuditActionType action, String entityType, String entityId,
                        Map<String, Object> previousValues, Map<String, Object> newValues,
                        String message) {
        AuditContext ctx = AuditContext.get();

        Map<String, Object> details = new HashMap<>();
        if (message != null) {
            details.put("message", message);
        }
        if (ctx.getMetadata() != null) {
            details.putAll(ctx.getMetadata());
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action.name())
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(ctx.getPerformedBy())
                .previousValues(previousValues)
                .newValues(newValues)
                .details(details)
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .correlationId(ctx.getCorrelationId())
                .sessionId(ctx.getSessionId())
                .requestId(ctx.getRequestId())
                .success(true)
                .immutable(true)
                .timestamp(Instant.now())
                .build();

        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to publish audit event: {}", e.getMessage());
        }
    }

    public void publishWithValues(AuditActionType action, String entityType, String entityId,
                                   Map<String, Object> previousValues, Map<String, Object> newValues,
                                   Map<String, Object> additionalMetadata) {
        AuditContext ctx = AuditContext.get();

        Map<String, Object> details = new HashMap<>();
        if (additionalMetadata != null) {
            details.putAll(additionalMetadata);
        }
        if (ctx.getMetadata() != null) {
            details.putAll(ctx.getMetadata());
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action.name())
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(ctx.getPerformedBy())
                .previousValues(previousValues)
                .newValues(newValues)
                .details(details)
                .ipAddress(ctx.getIpAddress())
                .userAgent(ctx.getUserAgent())
                .correlationId(ctx.getCorrelationId())
                .sessionId(ctx.getSessionId())
                .requestId(ctx.getRequestId())
                .success(true)
                .immutable(true)
                .timestamp(Instant.now())
                .build();

        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to publish audit event with values: {}", e.getMessage());
        }
    }
}
