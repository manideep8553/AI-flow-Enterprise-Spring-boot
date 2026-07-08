package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.WorkflowResponse;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowTemplate;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.mapper.WorkflowMapper;
import com.aiflow.enterprise.org.service.WorkflowTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflow-templates")
@Tag(name = "Workflow Templates", description = "Reusable workflow template APIs")
public class WorkflowTemplateController {

    private final WorkflowTemplateService service;
    private final WorkflowMapper workflowMapper;
    private final ObjectMapper objectMapper;

    public WorkflowTemplateController(WorkflowTemplateService service,
                                      WorkflowMapper workflowMapper,
                                      ObjectMapper objectMapper) {
        this.service = service;
        this.workflowMapper = workflowMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a workflow template")
    public ResponseEntity<ApiResponse<WorkflowTemplate>> create(
            @RequestBody TemplateCreateRequest request,
            @RequestParam(defaultValue = "system") String createdBy) {
        WorkflowTemplate template = service.create(
                request.name(), request.description(), request.category(),
                request.tags(), request.steps(), request.metadata(), createdBy);
        return new ResponseEntity<>(ApiResponse.created(template), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List workflow templates")
    public ResponseEntity<ApiResponse<PageResponse<WorkflowTemplate>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        Page<WorkflowTemplate> p = service.getAll(page, size, category, search);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(p, p.getContent())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID")
    public ResponseEntity<ApiResponse<WorkflowTemplate>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping("/{id}/use")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a workflow from a template")
    public ResponseEntity<ApiResponse<WorkflowResponse>> createFromTemplate(
            @PathVariable String id,
            @RequestParam(defaultValue = "system") String createdBy) {
        Workflow workflow = service.createWorkflowFromTemplate(id, createdBy);
        return new ResponseEntity<>(ApiResponse.created(workflowMapper.toResponse(workflow)), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish a template")
    public ResponseEntity<ApiResponse<Void>> publish(@PathVariable String id) {
        service.publishTemplate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Template published"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a template")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    private record TemplateCreateRequest(
            String name, String description, String category,
            List<String> tags, List<WorkflowStep> steps,
            Map<String, Object> metadata) {}
}
