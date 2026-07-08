package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.org.dto.request.BusinessUnitRequest;
import com.aiflow.enterprise.org.dto.response.BusinessUnitResponse;
import com.aiflow.enterprise.org.service.BusinessUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/business-units")
@Tag(name = "Business Units", description = "Business unit management APIs")
public class BusinessUnitController {

    private final BusinessUnitService service;

    public BusinessUnitController(BusinessUnitService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a business unit")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> create(
            @PathVariable String orgId, @Valid @RequestBody BusinessUnitRequest req) {
        req.setOrganizationId(orgId);
        return new ResponseEntity<>(ApiResponse.created(service.create(req)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update a business unit")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> update(
            @PathVariable String id, @Valid @RequestBody BusinessUnitRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get business unit by ID")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List business units")
    public ResponseEntity<ApiResponse<PageResponse<BusinessUnitResponse>>> getAll(
            @PathVariable String orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active) {
        Page<BusinessUnitResponse> p = service.getByOrganization(orgId, page, size, active);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(p, p.getContent())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a business unit")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
