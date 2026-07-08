package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.response.ComplianceReportResponse;
import org.springframework.data.domain.Page;

import java.time.Instant;

public interface ComplianceService {

    Page<ComplianceReportResponse> getReports(int page, int size, String reportType, String status);

    ComplianceReportResponse getReportById(String id);

    ComplianceReportResponse generateReport(String reportType, Instant from, Instant to, String generatedBy);

    ComplianceReportResponse generateUserActivityReport(Instant from, Instant to, String generatedBy);

    ComplianceReportResponse generateActionSummaryReport(Instant from, Instant to, String generatedBy);

    ComplianceReportResponse generateSecurityAuditReport(Instant from, Instant to, String generatedBy);

    ComplianceReportResponse generateWorkflowAuditReport(Instant from, Instant to, String generatedBy);

    ComplianceReportResponse generateDataAccessReport(Instant from, Instant to, String generatedBy);

    long getReportCount();
}
