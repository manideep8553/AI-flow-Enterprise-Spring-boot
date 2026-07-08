package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.FraudAlert;
import com.aiflow.enterprise.enums.FraudRiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends MongoRepository<FraudAlert, String> {

    Page<FraudAlert> findByUserId(String userId, Pageable pageable);

    Page<FraudAlert> findByDepartment(String department, Pageable pageable);

    Page<FraudAlert> findByRiskLevel(FraudRiskLevel riskLevel, Pageable pageable);

    Page<FraudAlert> findByResolvedFalse(Pageable pageable);

    Page<FraudAlert> findByAcknowledgedFalse(Pageable pageable);

    Page<FraudAlert> findByAssignedTo(String assignedTo, Pageable pageable);

    List<FraudAlert> findByResolvedFalseAndRiskLevel(FraudRiskLevel riskLevel);

    long countByResolvedFalse();

    long countByResolvedFalseAndRiskLevel(FraudRiskLevel riskLevel);
}
