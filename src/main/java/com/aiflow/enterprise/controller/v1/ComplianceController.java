package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.ComplianceReportResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.service.ComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "Compliance", description = "Compliance reporting and audit report generation APIs")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/reports")
    @Operation(summary = "List all compliance reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ComplianceReportResponse>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status) {
        Page<ComplianceReportResponse> reportPage = complianceService.getReports(page, size, reportType, status);
        PageResponse<ComplianceReportResponse> pageResponse = PageResponse.from(reportPage, reportPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/reports/{id}")
    @Operation(summary = "Get compliance report by ID")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> getReportById(@PathVariable String id) {
        ComplianceReportResponse report = complianceService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/generate")
    @Operation(summary = "Generate a compliance report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateReport(
            @RequestParam String reportType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        ComplianceReportResponse report = complianceService.generateReport(
                reportType, fromInstant, toInstant, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/user-activity")
    @Operation(summary = "Generate user activity compliance report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateUserActivityReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        ComplianceReportResponse report = complianceService.generateUserActivityReport(
                fromInstant, toInstant, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/security-audit")
    @Operation(summary = "Generate security audit report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateSecurityAuditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        ComplianceReportResponse report = complianceService.generateSecurityAuditReport(
                fromInstant, toInstant, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/workflow-audit")
    @Operation(summary = "Generate workflow audit report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateWorkflowAuditReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        ComplianceReportResponse report = complianceService.generateWorkflowAuditReport(
                fromInstant, toInstant, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @PostMapping("/reports/data-access")
    @Operation(summary = "Generate data access audit report")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComplianceReportResponse>> generateDataAccessReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails userDetails) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant = to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        ComplianceReportResponse report = complianceService.generateDataAccessReport(
                fromInstant, toInstant, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/reports/count")
    @Operation(summary = "Get total number of generated reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getReportCount() {
        return ResponseEntity.ok(ApiResponse.success(complianceService.getReportCount()));
    }
}
