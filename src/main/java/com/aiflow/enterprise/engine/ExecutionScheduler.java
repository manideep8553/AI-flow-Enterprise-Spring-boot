package com.aiflow.enterprise.engine;

import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.aiflow.enterprise.repository.WorkflowExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class ExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExecutionScheduler.class);

    private final TaskScheduler taskScheduler;
    private final WorkflowExecutionRepository executionRepository;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public ExecutionScheduler(TaskScheduler taskScheduler,
                              WorkflowExecutionRepository executionRepository) {
        this.taskScheduler = taskScheduler;
        this.executionRepository = executionRepository;
    }

    public void scheduleDelay(String executionId, String stepId, long delaySeconds, Runnable action) {
        cancelScheduled(executionId);
        log.info("Scheduling delay for execution {} step {} in {}s", executionId, stepId, delaySeconds);
        ScheduledFuture<?> future = taskScheduler.schedule(action,
                Instant.now().plusSeconds(delaySeconds));
        scheduledTasks.put(executionId + ":" + stepId, future);
    }

    public void scheduleRetry(String executionId, WorkflowStep step, long delaySeconds, Runnable action) {
        String key = executionId + ":retry:" + step.getStepId();
        cancelScheduled(key);
        log.info("Scheduling retry for execution {} step {} in {}s", executionId, step.getStepId(), delaySeconds);
        ScheduledFuture<?> future = taskScheduler.schedule(action,
                Instant.now().plusSeconds(delaySeconds));
        scheduledTasks.put(key, future);
    }

    public void scheduleTimeout(String executionId, String stepId, long timeoutSeconds, Runnable action) {
        String key = executionId + ":timeout:" + stepId;
        log.info("Scheduling timeout for execution {} step {} in {}s", executionId, stepId, timeoutSeconds);
        ScheduledFuture<?> future = taskScheduler.schedule(action,
                Instant.now().plusSeconds(timeoutSeconds));
        scheduledTasks.put(key, future);
    }

    public void scheduleCron(String executionId, String cronExpression, Runnable action) {
        cancelScheduled(executionId);
        log.info("Scheduling cron for execution {}: {}", executionId, cronExpression);
        try {
            CronTrigger trigger = new CronTrigger(cronExpression);
            ScheduledFuture<?> future = taskScheduler.schedule(action, trigger);
            scheduledTasks.put(executionId + ":cron", future);
        } catch (Exception e) {
            log.error("Invalid cron expression: {}", cronExpression, e);
        }
    }

    public void cancelScheduled(Object key) {
        ScheduledFuture<?> future = scheduledTasks.remove(key.toString());
        if (future != null && !future.isCancelled() && !future.isDone()) {
            future.cancel(false);
            log.debug("Cancelled scheduled task: {}", key);
        }
    }

    public void cancelAllForExecution(String executionId) {
        scheduledTasks.keySet().removeIf(key -> {
            if (key.startsWith(executionId)) {
                cancelScheduled(key);
                return true;
            }
            return false;
        });
    }

    public boolean isPendingRetry(String executionId, String stepId) {
        return scheduledTasks.containsKey(executionId + ":retry:" + stepId);
    }
}
