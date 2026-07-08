package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.org.dto.request.DepartmentRequest;
import com.aiflow.enterprise.org.dto.response.DepartmentResponse;
import com.aiflow.enterprise.org.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/departments")
@Tag(name = "Departments", description = "Department management APIs")
public class DepartmentController {

    private final DepartmentService service;

    public DepartmentController(DepartmentService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(
            @PathVariable String orgId, @Valid @RequestBody DepartmentRequest req) {
        req.setOrganizationId(orgId);
        return new ResponseEntity<>(ApiResponse.created(service.create(req)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable String orgId, @PathVariable String id, @Valid @RequestBody DepartmentRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List departments with pagination")
    public ResponseEntity<ApiResponse<PageResponse<DepartmentResponse>>> getAll(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        Page<DepartmentResponse> p = service.getAll(orgId, page, size, search, active);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(p, p.getContent())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a department")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Toggle department active status")
    public ResponseEntity<ApiResponse<DepartmentResponse>> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.toggleActive(id)));
    }
}
