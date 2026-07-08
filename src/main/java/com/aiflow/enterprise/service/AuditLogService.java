package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface AuditLogService {

    Page<AuditLogResponse> getAllAuditLogs(int page, int size, String entityType, String entityId,
                                           String performedBy, String action, Instant from, Instant to);

    AuditLogResponse getAuditLogById(String id);
}
