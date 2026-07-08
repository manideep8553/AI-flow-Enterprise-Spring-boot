package com.aiflow.enterprise.audit;

import com.aiflow.enterprise.entity.AuditLog;
import com.aiflow.enterprise.repository.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryBridge {

    private final AuditLogRepository auditLogRepository;

    public AuditLogRepositoryBridge(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public AuditLog save(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }
}
