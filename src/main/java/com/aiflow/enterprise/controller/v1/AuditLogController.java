package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Audit log retrieval APIs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @Operation(summary = "List audit logs with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAllAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @Parameter(description = "Start timestamp (ISO-8601)") Instant from,
            @RequestParam(required = false) @Parameter(description = "End timestamp (ISO-8601)") Instant to) {
        Page<AuditLogResponse> logPage = auditLogService.getAllAuditLogs(
                page, size, entityType, entityId, performedBy, action, from, to);
        PageResponse<AuditLogResponse> pageResponse = PageResponse.from(logPage, logPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable String id) {
        AuditLogResponse response = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
