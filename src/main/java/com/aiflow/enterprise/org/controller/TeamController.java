package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.org.dto.request.TeamRequest;
import com.aiflow.enterprise.org.dto.response.TeamResponse;
import com.aiflow.enterprise.org.service.TeamService;
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
@RequestMapping("/api/v1/departments/{deptId}/teams")
@Tag(name = "Teams", description = "Team management APIs")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Create a team")
    public ResponseEntity<ApiResponse<TeamResponse>> create(
            @PathVariable String deptId, @Valid @RequestBody TeamRequest req) {
        return new ResponseEntity<>(ApiResponse.created(service.create(req)), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
    @Operation(summary = "Update a team")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable String deptId, @PathVariable String id, @Valid @RequestBody TeamRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get team by ID")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List teams in a department")
    public ResponseEntity<ApiResponse<PageResponse<TeamResponse>>> getByDepartment(
            @PathVariable String deptId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        Page<TeamResponse> p = service.getByDepartment(deptId, page, size, search);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(p, p.getContent())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a team")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
