package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.ExecutionResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.service.WorkflowExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/executions")
@Tag(name = "Workflow Executions", description = "Workflow execution management APIs")
public class WorkflowExecutionController {

    private final WorkflowExecutionService executionService;

    public WorkflowExecutionController(WorkflowExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow execution by ID")
    public ResponseEntity<ApiResponse<ExecutionResponse>> getExecutionById(@PathVariable String id) {
        ExecutionResponse response = executionService.getExecutionById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List workflow executions with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<ExecutionResponse>>> getAllExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggeredBy) {
        Page<ExecutionResponse> executionPage = executionService.getAllExecutions(
                page, size, workflowId, status, triggeredBy);
        PageResponse<ExecutionResponse> pageResponse = PageResponse.from(
                executionPage, executionPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start a pending workflow execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> startExecution(@PathVariable String id) {
        ExecutionResponse response = executionService.startExecution(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Execution started"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a running workflow execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> cancelExecution(@PathVariable String id) {
        ExecutionResponse response = executionService.cancelExecution(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Execution cancelled"));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a running workflow execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> suspendExecution(@PathVariable String id) {
        ExecutionResponse response = executionService.suspendExecution(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Execution suspended"));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume a suspended workflow execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> resumeExecution(@PathVariable String id) {
        ExecutionResponse response = executionService.resumeExecution(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Execution resumed"));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed workflow execution from the beginning")
    public ResponseEntity<ApiResponse<ExecutionResponse>> retryExecution(@PathVariable String id) {
        ExecutionResponse response = executionService.retryExecution(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Execution retry initiated"));
    }

    @PostMapping("/{id}/retry-step/{stepId}")
    @Operation(summary = "Retry a specific failed step in the execution")
    public ResponseEntity<ApiResponse<ExecutionResponse>> retryStep(
            @PathVariable String id, @PathVariable String stepId) {
        ExecutionResponse response = executionService.retryStep(id, stepId);
        return ResponseEntity.ok(ApiResponse.success(response, "Step retry initiated"));
    }
}
