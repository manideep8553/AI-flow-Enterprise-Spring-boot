package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class ReportGenerationJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationJob.class);

    private final AuditLogService auditLogService;

    public ReportGenerationJob(DistributedLockService distributedLockService,
                                JobMonitorService jobMonitorService,
                                AuditLogService auditLogService) {
        super(distributedLockService, jobMonitorService);
        this.auditLogService = auditLogService;
    }

    @Override
    public String getJobName() {
        return "report-generator";
    }

    @Override
    public String getJobGroup() {
        return "reporting";
    }

    @Override
    public String getDescription() {
        return "Generates scheduled compliance and audit reports";
    }

    @Override
    public int getLockTtlSeconds() {
        return 600;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.report-generation.cron:0 0 2 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant now = Instant.now();
        String[] reportTypes = {"COMPLIANCE_SUMMARY", "AUDIT_ACTIVITY", "FRAUD_ANALYSIS", "WORKFLOW_PERFORMANCE"};

        for (String reportType : reportTypes) {
            try {
                Instant from;
                Instant to = now;

                switch (reportType) {
                    case "COMPLIANCE_SUMMARY" -> from = now.minus(7, ChronoUnit.DAYS);
                    case "AUDIT_ACTIVITY" -> from = now.minus(24, ChronoUnit.HOURS);
                    case "FRAUD_ANALYSIS" -> from = now.minus(30, ChronoUnit.DAYS);
                    case "WORKFLOW_PERFORMANCE" -> from = now.minus(7, ChronoUnit.DAYS);
                    default -> from = now.minus(24, ChronoUnit.HOURS);
                }

                var report = auditLogService.generateComplianceReport(reportType, from, to, "SCHEDULER");
                log.info("Report generated: type={} id={} records={}",
                        reportType, report.getId(), report.getRecordCount());

            } catch (Exception e) {
                log.error("Failed to generate report '{}': {}", reportType, e.getMessage());
            }
        }
    }
}
