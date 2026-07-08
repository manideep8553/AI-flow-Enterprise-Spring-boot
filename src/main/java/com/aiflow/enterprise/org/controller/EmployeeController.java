package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.org.dto.request.EmployeeProfileRequest;
import com.aiflow.enterprise.org.dto.response.EmployeeProfileResponse;
import com.aiflow.enterprise.org.service.EmployeeProfileService;
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
@RequestMapping("/api/v1/organizations/{orgId}/employees")
@Tag(name = "Employee Profiles", description = "Employee profile management APIs")
public class EmployeeController {

    private final EmployeeProfileService service;

    public EmployeeController(EmployeeProfileService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create an employee profile")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> create(
            @PathVariable String orgId, @Valid @RequestBody EmployeeProfileRequest req) {
        req.setOrganizationId(orgId);
        return new ResponseEntity<>(ApiResponse.created(service.create(req)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update an employee profile")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> update(
            @PathVariable String id, @Valid @RequestBody EmployeeProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get employee profile by ID")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get employee profile by user ID")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByUserId(userId)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List employees with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeProfileResponse>>> getAll(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        Page<EmployeeProfileResponse> p = service.getAll(orgId, page, size, departmentId, status, search);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(p, p.getContent())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update employee status (ACTIVE, TERMINATED, etc.)")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> updateStatus(
            @PathVariable String id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(service.updateStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an employee profile")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
