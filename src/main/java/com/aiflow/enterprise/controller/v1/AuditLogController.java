package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.AuditExportRequest;
import com.aiflow.enterprise.dto.request.AuditLogFilterRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.dto.response.AuditSummaryResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Logs", description = "Audit & Compliance log retrieval and management APIs")
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

    @PostMapping("/search")
    @Operation(summary = "Advanced search audit logs with complex filtering")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> searchAuditLogs(
            @RequestBody AuditLogFilterRequest filter) {
        Page<AuditLogResponse> logPage = auditLogService.getAuditLogs(filter);
        PageResponse<AuditLogResponse> pageResponse = PageResponse.from(logPage, logPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log by ID")
    public ResponseEntity<ApiResponse<AuditLogResponse>> getAuditLogById(@PathVariable String id) {
        AuditLogResponse response = auditLogService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/correlation/{correlationId}")
    @Operation(summary = "Get audit logs by correlation ID (tracing)")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByCorrelationId(
            @PathVariable String correlationId) {
        List<AuditLogResponse> logs = auditLogService.getAuditLogsByCorrelationId(correlationId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Get audit logs by session ID")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getBySessionId(
            @PathVariable String sessionId) {
        List<AuditLogResponse> logs = auditLogService.getAuditLogsBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get audit logs by entity type and ID")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        List<AuditLogResponse> logs = auditLogService.getAuditLogsByEntity(entityType, entityId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/workflow/{workflowId}")
    @Operation(summary = "Get audit logs by workflow ID")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByWorkflowId(
            @PathVariable String workflowId) {
        List<AuditLogResponse> logs = auditLogService.getAuditLogsByWorkflowId(workflowId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/execution/{executionId}")
    @Operation(summary = "Get audit logs by execution ID")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByExecutionId(
            @PathVariable String executionId) {
        List<AuditLogResponse> logs = auditLogService.getAuditLogsByExecutionId(executionId);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get audit summary statistics")
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        AuditSummaryResponse summary = auditLogService.getAuditSummary(fromInstant, toInstant);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get audit log statistics")
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getStats(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        AuditSummaryResponse summary = auditLogService.getAuditSummary(from, to);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/export")
    @Operation(summary = "Export audit logs to CSV or JSON")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportAuditLogs(@RequestBody AuditExportRequest request,
                                HttpServletResponse response) throws java.io.IOException {
        String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "csv";
        String contentType = format.equals("json") ? "application/json" : "text/csv";
        String extension = format.equals("json") ? "json" : "csv";

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename("audit-logs-export." + extension)
                        .build()
                        .toString());

        auditLogService.exportAuditLogs(request, response.getOutputStream());
    }

    @GetMapping("/count")
    @Operation(summary = "Get total audit log count")
    public ResponseEntity<ApiResponse<Long>> getCount() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getTotalAuditLogCount()));
    }

    @GetMapping("/failures/count")
    @Operation(summary = "Get total failure count")
    public ResponseEntity<ApiResponse<Long>> getFailureCount() {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getFailureCount()));
    }
}
