package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.org.dto.request.OrganizationSettingRequest;
import com.aiflow.enterprise.org.dto.response.OrganizationSettingResponse;
import com.aiflow.enterprise.org.service.OrganizationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/settings")
@Tag(name = "Organization Settings", description = "Organization-level settings APIs")
public class OrganizationSettingController {

    private final OrganizationSettingService service;

    public OrganizationSettingController(OrganizationSettingService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get organization settings")
    public ResponseEntity<ApiResponse<OrganizationSettingResponse>> get(@PathVariable String orgId) {
        return ResponseEntity.ok(ApiResponse.success(service.getByOrgId(orgId)));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update organization settings")
    public ResponseEntity<ApiResponse<OrganizationSettingResponse>> update(
            @PathVariable String orgId, @Valid @RequestBody OrganizationSettingRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(orgId, req)));
    }
}
