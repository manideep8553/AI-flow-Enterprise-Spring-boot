package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.response.ComplianceReportResponse;
import com.aiflow.enterprise.entity.ComplianceReport;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.repository.ComplianceReportRepository;
import com.aiflow.enterprise.service.AuditLogService;
import com.aiflow.enterprise.service.ComplianceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ComplianceServiceImpl implements ComplianceService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceServiceImpl.class);

    private final ComplianceReportRepository complianceReportRepository;
    private final AuditLogService auditLogService;

    public ComplianceServiceImpl(ComplianceReportRepository complianceReportRepository,
                                  AuditLogService auditLogService) {
        this.complianceReportRepository = complianceReportRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public Page<ComplianceReportResponse> getReports(int page, int size, String reportType, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "generatedAt"));
        Page<ComplianceReport> reportPage;

        if (reportType != null && status != null) {
            reportPage = complianceReportRepository.findByReportType(reportType, pageable);
        } else if (reportType != null) {
            reportPage = complianceReportRepository.findByReportType(reportType, pageable);
        } else if (status != null) {
            reportPage = complianceReportRepository.findByStatus(status, pageable);
        } else {
            reportPage = complianceReportRepository.findAll(pageable);
        }

        return reportPage.map(this::mapToResponse);
    }

    @Override
    public ComplianceReportResponse getReportById(String id) {
        ComplianceReport report = complianceReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ComplianceReport", "id", id));
        return mapToResponse(report);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateReport(String reportType, Instant from, Instant to, String generatedBy) {
        return auditLogService.generateComplianceReport(reportType, from, to, generatedBy);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateUserActivityReport(Instant from, Instant to, String generatedBy) {
        return generateReport("USER_ACTIVITY", from, to, generatedBy);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateActionSummaryReport(Instant from, Instant to, String generatedBy) {
        return generateReport("ACTION_SUMMARY", from, to, generatedBy);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateSecurityAuditReport(Instant from, Instant to, String generatedBy) {
        var summary = auditLogService.getAuditSummary(from, to);

        Map<String, Object> reportSummary = new HashMap<>();
        reportSummary.put("totalLogs", summary.getTotalLogs());
        reportSummary.put("successCount", summary.getSuccessCount());
        reportSummary.put("failureCount", summary.getFailureCount());
        reportSummary.put("uniqueUsers", summary.getUniqueUsers());
        reportSummary.put("actionBreakdown", summary.getActionBreakdown());

        Map<String, Object> securityMetrics = new HashMap<>();
        securityMetrics.put("loginFailures", summary.getActionBreakdown().getOrDefault("LOGIN_FAILED", 0L));
        securityMetrics.put("accessDenied", summary.getActionBreakdown().getOrDefault("ACCESS_DENIED", 0L));
        securityMetrics.put("passwordChanges", summary.getActionBreakdown().getOrDefault("PASSWORD_CHANGED", 0L)
                + summary.getActionBreakdown().getOrDefault("PASSWORD_RESET", 0L));
        reportSummary.put("securityMetrics", securityMetrics);

        ComplianceReport report = ComplianceReport.builder()
                .reportType("SECURITY_AUDIT")
                .title("Security Audit Report")
                .description("Comprehensive security audit report covering authentication, access control, and security events")
                .parameters(Map.of("from", from != null ? from.toString() : null, "to", to != null ? to.toString() : null))
                .summary(reportSummary)
                .status("COMPLETED")
                .format("JSON")
                .recordCount(summary.getTotalLogs())
                .generatedBy(generatedBy)
                .from(from)
                .to(to)
                .generatedAt(Instant.now())
                .build();

        report = complianceReportRepository.save(report);
        return mapToResponse(report);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateWorkflowAuditReport(Instant from, Instant to, String generatedBy) {
        var summary = auditLogService.getAuditSummary(from, to);

        Map<String, Object> reportSummary = new HashMap<>();
        reportSummary.put("totalLogs", summary.getTotalLogs());
        reportSummary.put("actionBreakdown", summary.getActionBreakdown());

        Map<String, Object> workflowMetrics = new HashMap<>();
        long workflowCreations = summary.getActionBreakdown().getOrDefault("WORKFLOW_CREATED", 0L);
        long workflowUpdates = summary.getActionBreakdown().getOrDefault("WORKFLOW_UPDATED", 0L);
        long workflowDeletions = summary.getActionBreakdown().getOrDefault("WORKFLOW_DELETED", 0L);
        long executions = summary.getActionBreakdown().getOrDefault("EXECUTION_STARTED", 0L);
        long approvals = summary.getActionBreakdown().getOrDefault("APPROVAL_APPROVED", 0L);
        long rejections = summary.getActionBreakdown().getOrDefault("APPROVAL_REJECTED", 0L);

        workflowMetrics.put("workflowCreations", workflowCreations);
        workflowMetrics.put("workflowUpdates", workflowUpdates);
        workflowMetrics.put("workflowDeletions", workflowDeletions);
        workflowMetrics.put("executionsStarted", executions);
        workflowMetrics.put("approvals", approvals);
        workflowMetrics.put("rejections", rejections);
        reportSummary.put("workflowMetrics", workflowMetrics);

        ComplianceReport report = ComplianceReport.builder()
                .reportType("WORKFLOW_AUDIT")
                .title("Workflow Audit Report")
                .description("Audit report focused on workflow operations, executions, and approvals")
                .parameters(Map.of("from", from != null ? from.toString() : null, "to", to != null ? to.toString() : null))
                .summary(reportSummary)
                .status("COMPLETED")
                .format("JSON")
                .recordCount(summary.getTotalLogs())
                .generatedBy(generatedBy)
                .from(from)
                .to(to)
                .generatedAt(Instant.now())
                .build();

        report = complianceReportRepository.save(report);
        return mapToResponse(report);
    }

    @Override
    @Transactional
    public ComplianceReportResponse generateDataAccessReport(Instant from, Instant to, String generatedBy) {
        var summary = auditLogService.getAuditSummary(from, to);

        Map<String, Object> reportSummary = new HashMap<>();
        reportSummary.put("totalLogs", summary.getTotalLogs());
        reportSummary.put("actionBreakdown", summary.getActionBreakdown());
        reportSummary.put("entityTypeBreakdown", summary.getEntityTypeBreakdown());

        Map<String, Object> dataMetrics = new HashMap<>();
        dataMetrics.put("dataCreations", summary.getActionBreakdown().getOrDefault("DATA_CREATED", 0L));
        dataMetrics.put("dataUpdates", summary.getActionBreakdown().getOrDefault("DATA_UPDATED", 0L));
        dataMetrics.put("dataDeletions", summary.getActionBreakdown().getOrDefault("DATA_DELETED", 0L));
        dataMetrics.put("dataExports", summary.getActionBreakdown().getOrDefault("DATA_EXPORTED", 0L));
        dataMetrics.put("dataImports", summary.getActionBreakdown().getOrDefault("DATA_IMPORTED", 0L));
        dataMetrics.put("documentUploads", summary.getActionBreakdown().getOrDefault("DOCUMENT_UPLOADED", 0L));
        dataMetrics.put("documentDownloads", summary.getActionBreakdown().getOrDefault("DOCUMENT_DOWNLOADED", 0L));
        reportSummary.put("dataMetrics", dataMetrics);

        ComplianceReport report = ComplianceReport.builder()
                .reportType("DATA_ACCESS")
                .title("Data Access Audit Report")
                .description("Audit report covering data creation, update, deletion, export, and document access")
                .parameters(Map.of("from", from != null ? from.toString() : null, "to", to != null ? to.toString() : null))
                .summary(reportSummary)
                .status("COMPLETED")
                .format("JSON")
                .recordCount(summary.getTotalLogs())
                .generatedBy(generatedBy)
                .from(from)
                .to(to)
                .generatedAt(Instant.now())
                .build();

        report = complianceReportRepository.save(report);
        return mapToResponse(report);
    }

    @Override
    public long getReportCount() {
        return complianceReportRepository.count();
    }

    private ComplianceReportResponse mapToResponse(ComplianceReport report) {
        return ComplianceReportResponse.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .title(report.getTitle())
                .description(report.getDescription())
                .parameters(report.getParameters())
                .summary(report.getSummary())
                .status(report.getStatus())
                .format(report.getFormat())
                .recordCount(report.getRecordCount())
                .generatedBy(report.getGeneratedBy())
                .from(report.getFrom())
                .to(report.getTo())
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .downloadUrl(report.getDownloadUrl())
                .build();
    }
}
