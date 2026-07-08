package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.ai.AIRequestType;
import com.aiflow.enterprise.entity.AIDecisionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIDecisionLogRepository extends MongoRepository<AIDecisionLog, String> {

    Page<AIDecisionLog> findByUserId(String userId, Pageable pageable);

    Page<AIDecisionLog> findByRequestType(AIRequestType requestType, Pageable pageable);

    Page<AIDecisionLog> findByWorkflowId(String workflowId, Pageable pageable);

    Page<AIDecisionLog> findByExecutionId(String executionId, Pageable pageable);

    List<AIDecisionLog> findByRequestIdOrderByCreatedAtDesc(String requestId);

    long countByRequestType(AIRequestType requestType);

    long countByConfidenceLevel(com.aiflow.enterprise.ai.ConfidenceLevel confidenceLevel);

    long countByAccepted(boolean accepted);
}
