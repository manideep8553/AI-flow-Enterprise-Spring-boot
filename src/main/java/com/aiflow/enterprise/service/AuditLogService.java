package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.AuditExportRequest;
import com.aiflow.enterprise.dto.request.AuditLogFilterRequest;
import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.dto.response.AuditSummaryResponse;
import com.aiflow.enterprise.dto.response.ComplianceReportResponse;
import org.springframework.data.domain.Page;

import java.io.OutputStream;
import java.time.Instant;
import java.util.List;

public interface AuditLogService {

    Page<AuditLogResponse> getAllAuditLogs(int page, int size, String entityType, String entityId,
                                           String performedBy, String action, Instant from, Instant to);

    Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest filter);

    AuditLogResponse getAuditLogById(String id);

    List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId);

    List<AuditLogResponse> getAuditLogsBySessionId(String sessionId);

    List<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId);

    List<AuditLogResponse> getAuditLogsByWorkflowId(String workflowId);

    List<AuditLogResponse> getAuditLogsByExecutionId(String executionId);

    AuditSummaryResponse getAuditSummary(Instant from, Instant to);

    void exportAuditLogs(AuditExportRequest request, OutputStream outputStream);

    ComplianceReportResponse generateComplianceReport(String reportType, Instant from, Instant to, String generatedBy);

    long getTotalAuditLogCount();

    long getFailureCount();
}
