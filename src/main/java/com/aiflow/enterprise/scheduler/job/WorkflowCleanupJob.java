package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class WorkflowCleanupJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCleanupJob.class);

    private final MongoTemplate mongoTemplate;

    public WorkflowCleanupJob(DistributedLockService distributedLockService,
                               JobMonitorService jobMonitorService,
                               MongoTemplate mongoTemplate) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getJobName() {
        return "workflow-cleanup";
    }

    @Override
    public String getJobGroup() {
        return "maintenance";
    }

    @Override
    public String getDescription() {
        return "Cleans up stale, expired, and completed workflow executions beyond retention period";
    }

    @Override
    public int getLockTtlSeconds() {
        return 300;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.workflow-cleanup.cron:0 0 3 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant now = Instant.now();
        int totalArchived = 0;
        int totalFailedStuck = 0;

        Instant retentionCutoff = now.minus(90, ChronoUnit.DAYS);

        Query deleteCompleted = Query.query(Criteria.where("status").in(
                        ExecutionStatus.COMPLETED.name(),
                        ExecutionStatus.FAILED.name(),
                        ExecutionStatus.CANCELLED.name())
                .and("startedAt").lt(retentionCutoff));

        long completedDeleted = mongoTemplate.remove(deleteCompleted, WorkflowExecution.class).getDeletedCount();
        totalArchived += completedDeleted;
        log.info("Archived {} completed workflow executions older than 90 days", completedDeleted);

        Instant stuckThreshold = now.minus(24, ChronoUnit.HOURS);
        Query failStuck = Query.query(Criteria.where("status").is(ExecutionStatus.RUNNING.name())
                .and("startedAt").lt(stuckThreshold));

        var stuckExecutions = mongoTemplate.find(failStuck, WorkflowExecution.class);
        for (var execution : stuckExecutions) {
            try {
                execution.setStatus(ExecutionStatus.FAILED);
                execution.setCompletedAt(now);
                execution.setErrorMessage("Auto-terminated after 24h of inactivity");
                mongoTemplate.save(execution);
                totalFailedStuck++;
            } catch (Exception e) {
                log.warn("Failed to terminate stuck execution {}: {}", execution.getId(), e.getMessage());
            }
        }

        log.info("Workflow cleanup complete: archived={}, terminated-stuck={}",
                totalArchived, totalFailedStuck);
    }
}
