package com.aiflow.enterprise.scheduler.batch;

import com.aiflow.enterprise.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ReportGenerationTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationTasklet.class);

    private final AuditLogService auditLogService;

    public ReportGenerationTasklet(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Instant now = Instant.now();
        Instant from = now.minus(30, ChronoUnit.DAYS);

        String[] reportTypes = {"COMPLIANCE_SUMMARY", "FRAUD_ANALYSIS", "WORKFLOW_PERFORMANCE"};

        int generated = 0;
        for (String reportType : reportTypes) {
            try {
                var report = auditLogService.generateComplianceReport(reportType, from, now, "BATCH_JOB");
                log.info("Batch report generated: type={} id={} records={}",
                        reportType, report.getId(), report.getRecordCount());
                generated++;
            } catch (Exception e) {
                log.error("Batch report generation failed for '{}': {}", reportType, e.getMessage());
                contribution.incrementWriteCount(0);
            }
        }

        log.info("Report generation tasklet complete: {} reports generated", generated);
        return RepeatStatus.FINISHED;
    }
}
