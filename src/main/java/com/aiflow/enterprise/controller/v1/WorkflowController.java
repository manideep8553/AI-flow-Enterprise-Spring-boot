package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.ExecutionRequest;
import com.aiflow.enterprise.dto.request.WorkflowRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.WorkflowResponse;
import com.aiflow.enterprise.service.WorkflowExportImportService;
import com.aiflow.enterprise.service.WorkflowService;
import com.aiflow.enterprise.service.WorkflowValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows", description = "Workflow management APIs including visual builder support")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowValidationService validationService;
    private final WorkflowExportImportService exportImportService;

    public WorkflowController(WorkflowService workflowService,
                              WorkflowValidationService validationService,
                              WorkflowExportImportService exportImportService) {
        this.workflowService = workflowService;
        this.validationService = validationService;
        this.exportImportService = exportImportService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a new workflow", description = "Creates a workflow in DRAFT status")
    public ResponseEntity<ApiResponse<WorkflowResponse>> createWorkflow(
            @Valid @RequestBody WorkflowRequest request,
            @RequestParam(defaultValue = "system") String createdBy) {
        WorkflowResponse response = workflowService.createWorkflow(request, createdBy);
        return new ResponseEntity<>(ApiResponse.created(response), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update an existing workflow")
    public ResponseEntity<ApiResponse<WorkflowResponse>> updateWorkflow(
            @PathVariable String id,
            @Valid @RequestBody WorkflowRequest request) {
        WorkflowResponse response = workflowService.updateWorkflow(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow updated successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get workflow by ID")
    public ResponseEntity<ApiResponse<WorkflowResponse>> getWorkflowById(@PathVariable String id) {
        WorkflowResponse response = workflowService.getWorkflowById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List workflows with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowResponse>>> getAllWorkflows(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String category) {
        Page<WorkflowResponse> workflowPage = workflowService.getAllWorkflows(
                page, size, status, search, tag, category);
        PageResponse<WorkflowResponse> pageResponse = PageResponse.from(workflowPage, workflowPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a workflow")
    public ResponseEntity<ApiResponse<Void>> deleteWorkflow(@PathVariable String id) {
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Publish a workflow", description = "Validates then changes status to PUBLISHED")
    public ResponseEntity<ApiResponse<WorkflowResponse>> publishWorkflow(@PathVariable String id) {
        WorkflowResponse response = workflowService.publishWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow published successfully"));
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Archive a workflow", description = "Changes workflow status to ARCHIVED")
    public ResponseEntity<ApiResponse<WorkflowResponse>> archiveWorkflow(@PathVariable String id) {
        WorkflowResponse response = workflowService.archiveWorkflow(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow archived successfully"));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Execute a workflow", description = "Creates a new execution for a published workflow")
    public ResponseEntity<ApiResponse<Map<String, String>>> executeWorkflow(
            @PathVariable String id,
            @Valid @RequestBody ExecutionRequest request) {
        String executionId = workflowService.executeWorkflow(
                id, request.getTriggeredBy(), request.getInputParams());
        Map<String, String> data = Map.of("executionId", executionId);
        return ResponseEntity.ok(ApiResponse.success(data, "Workflow execution started"));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Validate workflow steps and topology")
    public ResponseEntity<ApiResponse<WorkflowValidationService.ValidationResult>> validateWorkflow(
            @PathVariable String id) {
        WorkflowResponse workflow = workflowService.getWorkflowById(id);
        var result = validationService.validate(
                workflow.getSteps().stream()
                        .map(s -> com.aiflow.enterprise.entity.embedded.WorkflowStep.builder()
                                .stepId(s.getStepId()).name(s.getName()).type(s.getType())
                                .order(s.getOrder()).config(s.getConfig())
                                .dependsOn(s.getDependsOn()).build())
                        .toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/rollback/{version}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Rollback workflow to a previous version")
    public ResponseEntity<ApiResponse<WorkflowResponse>> rollbackWorkflow(
            @PathVariable String id, @PathVariable Integer version) {
        WorkflowResponse response = workflowService.rollbackToVersion(id, version);
        return ResponseEntity.ok(ApiResponse.success(response, "Workflow rolled back to version " + version));
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export workflow as JSON")
    public ResponseEntity<String> exportWorkflow(@PathVariable String id) {
        String json = exportImportService.exportToJson(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", "attachment; filename=\"workflow.json\"")
                .body(json);
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Import workflow from JSON")
    public ResponseEntity<ApiResponse<WorkflowResponse>> importWorkflow(
            @RequestBody String json,
            @RequestParam(defaultValue = "system") String createdBy) {
        var workflow = exportImportService.importFromJson(json, createdBy);
        return new ResponseEntity<>(
                ApiResponse.created(
                        workflowService.getWorkflowById(workflow.getId())),
                HttpStatus.CREATED);
    }

    @GetMapping("/versions/{workflowId}")
    @Operation(summary = "Get all workflow versions")
    public ResponseEntity<ApiResponse<List<Integer>>> getVersions(@PathVariable String workflowId) {
        List<Integer> versions = workflowService.getWorkflowVersions(workflowId);
        return ResponseEntity.ok(ApiResponse.success(versions));
    }
}
