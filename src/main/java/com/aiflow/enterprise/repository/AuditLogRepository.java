package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<AuditLog> findByPerformedBy(String performedBy, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByActionIn(List<String> actions, Pageable pageable);

    Page<AuditLog> findByTimestampBetween(Instant from, Instant to, Pageable pageable);

    Page<AuditLog> findByCorrelationId(String correlationId, Pageable pageable);

    Page<AuditLog> findBySessionId(String sessionId, Pageable pageable);

    Page<AuditLog> findByRequestId(String requestId, Pageable pageable);

    Page<AuditLog> findByWorkflowId(String workflowId, Pageable pageable);

    Page<AuditLog> findByExecutionId(String executionId, Pageable pageable);

    Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);

    Page<AuditLog> findBySuccess(boolean success, Pageable pageable);

    Page<AuditLog> findByImmutable(boolean immutable, Pageable pageable);

    Page<AuditLog> findByOrganizationId(String organizationId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    Page<AuditLog> findByPerformedByAndAction(String performedBy, String action, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndAction(String entityType, String action, Pageable pageable);

    Page<AuditLog> findByPerformedByAndTimestampBetween(String performedBy, Instant from, Instant to, Pageable pageable);

    Page<AuditLog> findByActionAndTimestampBetween(String action, Instant from, Instant to, Pageable pageable);

    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    List<AuditLog> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<AuditLog> findByTimestampBetweenOrderByTimestampAsc(Instant from, Instant to);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    long countByAction(String action);

    long countByPerformedBy(String performedBy);

    long countBySuccess(boolean success);

    long countByActionAndTimestampBetween(String action, Instant from, Instant to);

    long countByTimestampBetween(Instant from, Instant to);

    @Aggregation(pipeline = {
            "{ $group: { _id: '$action', count: { $sum: 1 } } }",
            "{ $sort: { count: -1 } }"
    })
    List<ActionCount> countByActionGrouped();

    @Aggregation(pipeline = {
            "{ $group: { _id: '$entityType', count: { $sum: 1 } } }",
            "{ $sort: { count: -1 } }"
    })
    List<EntityTypeCount> countByEntityTypeGrouped();

    @Aggregation(pipeline = {
            "{ $group: { _id: '$performedBy', count: { $sum: 1 } } }",
            "{ $sort: { count: -1 } }",
            "{ $limit: 10 }"
    })
    List<UserActivityCount> findTopActiveUsers();

    @Aggregation(pipeline = {
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$timestamp' } }, count: { $sum: 1 }, successCount: { $sum: { $cond: ['$success', 1, 0] } }, failureCount: { $sum: { $cond: [{ $not: '$success' }, 1, 0] } } } }",
            "{ $sort: { _id: 1 } }"
    })
    List<DailyAuditStats> getDailyAuditStats();

    @Aggregation(pipeline = {
            "{ $match: { timestamp: { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: { $dateToString: { format: '%Y-%m-%d', date: '$timestamp' } }, count: { $sum: 1 }, successCount: { $sum: { $cond: ['$success', 1, 0] } }, failureCount: { $sum: { $cond: [{ $not: '$success' }, 1, 0] } } } }",
            "{ $sort: { _id: 1 } }"
    })
    List<DailyAuditStats> getDailyAuditStatsBetween(Instant from, Instant to);

    interface ActionCount {
        String get_Id();
        long getCount();
    }

    interface EntityTypeCount {
        String get_Id();
        long getCount();
    }

    interface UserActivityCount {
        String get_Id();
        long getCount();
    }

    interface DailyAuditStats {
        String get_Id();
        long getCount();
        long getSuccessCount();
        long getFailureCount();
    }
}
