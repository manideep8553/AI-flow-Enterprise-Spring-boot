package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.FraudCheck;
import com.aiflow.enterprise.enums.FraudRiskLevel;
import com.aiflow.enterprise.enums.FraudStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FraudCheckRepository extends MongoRepository<FraudCheck, String> {

    Page<FraudCheck> findByUserId(String userId, Pageable pageable);

    Page<FraudCheck> findByDepartment(String department, Pageable pageable);

    Page<FraudCheck> findByRiskLevel(FraudRiskLevel riskLevel, Pageable pageable);

    Page<FraudCheck> findByStatus(FraudStatus status, Pageable pageable);

    Page<FraudCheck> findByRequestId(String requestId, Pageable pageable);

    Page<FraudCheck> findByEscalatedTrue(Pageable pageable);

    List<FraudCheck> findByUserIdOrderByCheckedAtDesc(String userId);

    List<FraudCheck> findByDepartmentAndCheckedAtAfter(String department, Instant after);

    List<FraudCheck> findByVendorAndCheckedAtAfter(String vendor, Instant after);

    long countByRiskLevel(FraudRiskLevel riskLevel);

    long countByStatus(FraudStatus status);

    long countByEscalatedTrue();

    long countByDepartmentAndCheckedAtAfter(String department, Instant after);
}
