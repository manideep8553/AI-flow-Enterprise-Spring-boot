package com.aiflow.enterprise.scheduler.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneOffset;

@Component
public class BatchJobCompletionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchJobCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("Batch job [{}] instance={} starting",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        long duration = 0;

        if (jobExecution.getEndTime() != null && jobExecution.getStartTime() != null) {
            duration = Duration.between(
                    jobExecution.getStartTime().toInstant(ZoneOffset.UTC),
                    jobExecution.getEndTime().toInstant(ZoneOffset.UTC)
            ).toMillis();
        }

        if (status == BatchStatus.COMPLETED) {
            log.info("Batch job [{}] completed successfully in {}ms", jobName, duration);
        } else if (status == BatchStatus.FAILED) {
            log.error("Batch job [{}] failed in {}ms. Exit status: {}",
                    jobName, duration, jobExecution.getExitStatus().getExitDescription());

            if (jobExecution.getAllFailureExceptions() != null) {
                jobExecution.getAllFailureExceptions().forEach(e ->
                        log.error("Batch job [{}] failure cause: {}", jobName, e.getMessage(), e));
            }
        } else {
            log.warn("Batch job [{}] finished with status {} in {}ms", jobName, status, duration);
        }
    }
}
