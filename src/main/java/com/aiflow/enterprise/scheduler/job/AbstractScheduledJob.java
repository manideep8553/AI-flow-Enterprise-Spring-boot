package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.model.JobExecutionRecord;
import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

public abstract class AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(AbstractScheduledJob.class);

    protected final DistributedLockService distributedLockService;
    protected final JobMonitorService jobMonitorService;

    protected AbstractScheduledJob(DistributedLockService distributedLockService,
                                    JobMonitorService jobMonitorService) {
        this.distributedLockService = distributedLockService;
        this.jobMonitorService = jobMonitorService;
    }

    public abstract String getJobName();

    public abstract String getJobGroup();

    public abstract String getDescription();

    public abstract int getLockTtlSeconds();

    public abstract int getMaxRetries();

    protected abstract void execute() throws Exception;

    public void run() {
        String lockKey = "job-lock:" + getJobName();

        if (!distributedLockService.acquireLock(lockKey, getLockTtlSeconds())) {
            log.debug("Job [{}] skipped - another instance holds the lock", getJobName());
            return;
        }

        JobExecutionRecord record = null;
        int retryCount = 0;

        try {
            record = jobMonitorService.startExecution(
                    getJobName(), getJobGroup(), getDescription(), "SCHEDULER", getMaxRetries());

            Instant start = Instant.now();
            execute();
            long duration = Duration.between(start, Instant.now()).toMillis();

            jobMonitorService.completeExecution(record.getId(), true,
                    "Completed in " + duration + "ms");
            log.info("Job [{}] completed successfully in {}ms", getJobName(), duration);

        } catch (Exception e) {
            log.error("Job [{}] failed: {}", getJobName(), e.getMessage(), e);

            if (record != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                jobMonitorService.failExecution(record.getId(), e.getMessage(), sw.toString());
            }

            if (getMaxRetries() > 0) {
                handleRetry(lockKey, record);
            }
        } finally {
            distributedLockService.releaseLock(lockKey);
        }
    }

    protected void runWithLock() {
        run();
    }

    public void runManual(String triggeredBy) {
        String lockKey = "job-lock:" + getJobName();
        if (!distributedLockService.acquireLock(lockKey, getLockTtlSeconds())) {
            log.warn("Manual trigger for [{}] failed - lock held by another instance", getJobName());
            return;
        }

        JobExecutionRecord record = null;
        try {
            record = jobMonitorService.startExecution(
                    getJobName(), getJobGroup(), getDescription(), triggeredBy, getMaxRetries());

            Instant start = Instant.now();
            execute();
            long duration = Duration.between(start, Instant.now()).toMillis();

            jobMonitorService.completeExecution(record.getId(), true,
                    "Manual trigger - completed in " + duration + "ms");
        } catch (Exception e) {
            if (record != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                jobMonitorService.failExecution(record.getId(), e.getMessage(), sw.toString());
            }
        } finally {
            distributedLockService.releaseLock(lockKey);
        }
    }

    private void handleRetry(String lockKey, JobExecutionRecord record) {
        for (int i = 1; i <= getMaxRetries(); i++) {
            log.info("Retry {}/{} for job [{}]", i, getMaxRetries(), getJobName());

            if (!distributedLockService.acquireLock(lockKey, getLockTtlSeconds())) {
                log.warn("Retry {}/{} for [{}] skipped - lock not available", i, getMaxRetries(), getJobName());
                return;
            }

            try {
                if (record != null) {
                    jobMonitorService.updateRetryCount(record.getId(), i);
                }

                execute();

                if (record != null) {
                    jobMonitorService.completeExecution(record.getId(), true,
                            "Completed on retry " + i);
                }
                log.info("Job [{}] succeeded on retry {}/{}", getJobName(), i, getMaxRetries());
                return;

            } catch (Exception e) {
                log.error("Job [{}] failed on retry {}/{}: {}", getJobName(), i, getMaxRetries(), e.getMessage());
                if (record != null) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    jobMonitorService.failExecution(record.getId(), e.getMessage(), sw.toString());
                }
            } finally {
                distributedLockService.releaseLock(lockKey);
            }

            try {
                Thread.sleep((long) Math.pow(2, i) * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
