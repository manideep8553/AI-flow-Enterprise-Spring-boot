package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.enums.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends MongoRepository<WorkflowExecution, String> {

    Page<WorkflowExecution> findByWorkflowId(String workflowId, Pageable pageable);

    Page<WorkflowExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    Page<WorkflowExecution> findByTriggeredBy(String triggeredBy, Pageable pageable);

    Page<WorkflowExecution> findByWorkflowIdAndStatus(String workflowId, ExecutionStatus status, Pageable pageable);

    List<WorkflowExecution> findByWorkflowIdOrderByCreatedAtDesc(String workflowId);

    List<WorkflowExecution> findByStatusIn(List<ExecutionStatus> statuses);

    long countByWorkflowIdAndStatus(String workflowId, ExecutionStatus status);

    long countByStatus(ExecutionStatus status);
}
