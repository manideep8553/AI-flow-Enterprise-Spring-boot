package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.entity.AuditLog;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.AuditLogMapper;
import com.aiflow.enterprise.repository.AuditLogRepository;
import com.aiflow.enterprise.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Override
    public Page<AuditLogResponse> getAllAuditLogs(int page, int size, String entityType,
                                                   String entityId, String performedBy,
                                                   String action, Instant from, Instant to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogPage;

        if (entityType != null) {
            auditLogPage = auditLogRepository.findByEntityType(entityType, pageable);
        } else if (entityId != null) {
            auditLogPage = auditLogRepository.findByEntityId(entityId, pageable);
        } else if (performedBy != null) {
            auditLogPage = auditLogRepository.findByPerformedBy(performedBy, pageable);
        } else if (action != null) {
            auditLogPage = auditLogRepository.findByAction(action, pageable);
        } else if (from != null && to != null) {
            auditLogPage = auditLogRepository.findByTimestampBetween(from, to, pageable);
        } else {
            auditLogPage = auditLogRepository.findAll(pageable);
        }

        return auditLogPage.map(auditLogMapper::toResponse);
    }

    @Override
    public AuditLogResponse getAuditLogById(String id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return auditLogMapper.toResponse(auditLog);
    }
}
