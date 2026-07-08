package com.aiflow.enterprise.scheduler.batch;

import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.enums.ExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class WorkflowCleanupTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(WorkflowCleanupTasklet.class);

    private final MongoTemplate mongoTemplate;

    public WorkflowCleanupTasklet(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Instant now = Instant.now();

        Instant executionRetention = now.minus(180, ChronoUnit.DAYS);
        Query deleteOldExecutions = Query.query(Criteria.where("status").in(
                        ExecutionStatus.COMPLETED.name(),
                        ExecutionStatus.FAILED.name(),
                        ExecutionStatus.CANCELLED.name())
                .and("startedAt").lt(executionRetention));
        long executionsRemoved = mongoTemplate.remove(deleteOldExecutions, WorkflowExecution.class).getDeletedCount();
        log.info("Batch cleanup: removed {} old workflow executions", executionsRemoved);

        Instant staleLockThreshold = now.minus(24, ChronoUnit.HOURS);
        Query deleteStaleLocks = Query.query(Criteria.where("expiresAt").lt(staleLockThreshold));
        long locksRemoved = mongoTemplate.remove(deleteStaleLocks, "scheduler_locks").getDeletedCount();
        if (locksRemoved > 0) {
            log.info("Batch cleanup: removed {} stale scheduler locks", locksRemoved);
        }

        Query failStuckExecutions = Query.query(Criteria.where("status").is(ExecutionStatus.RUNNING.name())
                .and("startedAt").lt(now.minus(48, ChronoUnit.HOURS)));
        var stuckExecutions = mongoTemplate.find(failStuckExecutions, WorkflowExecution.class);
        for (var execution : stuckExecutions) {
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setCompletedAt(now);
            execution.setErrorMessage("Batch cleanup - execution exceeded 48h limit");
            mongoTemplate.save(execution);
        }
        log.info("Batch cleanup: terminated {} stuck executions", stuckExecutions.size());

        contribution.incrementWriteCount((int) (executionsRemoved + locksRemoved + stuckExecutions.size()));
        return RepeatStatus.FINISHED;
    }
}
