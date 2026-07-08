package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.response.ExecutionResponse;
import com.aiflow.enterprise.entity.Workflow;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface WorkflowExecutionService {

    ExecutionResponse getExecutionById(String id);

    Page<ExecutionResponse> getAllExecutions(int page, int size, String workflowId, String status, String triggeredBy);

    String createExecution(Workflow workflow, String triggeredBy, Map<String, Object> inputParams);

    ExecutionResponse startExecution(String id);

    ExecutionResponse cancelExecution(String id);

    ExecutionResponse suspendExecution(String id);

    ExecutionResponse resumeExecution(String id);

    ExecutionResponse retryStep(String executionId, String stepId);

    ExecutionResponse retryExecution(String id);
}
