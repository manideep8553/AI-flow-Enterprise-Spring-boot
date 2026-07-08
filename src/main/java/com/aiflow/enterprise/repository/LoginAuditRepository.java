package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.LoginAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LoginAuditRepository extends MongoRepository<LoginAudit, String> {

    Page<LoginAudit> findByUserId(String userId, Pageable pageable);

    Page<LoginAudit> findByEmail(String email, Pageable pageable);

    Page<LoginAudit> findByAction(String action, Pageable pageable);

    List<LoginAudit> findByUserIdAndTimestampAfterOrderByTimestampDesc(String userId, Instant timestamp);

    long countByUserIdAndSuccessAndTimestampAfter(String userId, boolean success, Instant timestamp);

    long countByEmailAndSuccessAndTimestampAfter(String email, boolean success, Instant timestamp);

    Page<LoginAudit> findByTimestampBetween(Instant from, Instant to, Pageable pageable);
}
