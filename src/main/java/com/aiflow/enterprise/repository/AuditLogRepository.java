package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(Instant from, Instant to, Pageable pageable);
}
