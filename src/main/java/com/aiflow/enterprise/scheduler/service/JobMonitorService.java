package com.aiflow.enterprise.scheduler.service;

import com.aiflow.enterprise.scheduler.model.JobExecutionRecord;
import com.aiflow.enterprise.scheduler.model.JobStatus;
import com.aiflow.enterprise.scheduler.repository.JobExecutionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobMonitorService {

    private static final Logger log = LoggerFactory.getLogger(JobMonitorService.class);

    private final JobExecutionRecordRepository recordRepository;

    public JobMonitorService(JobExecutionRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public JobExecutionRecord startExecution(String jobName, String jobGroup, String description,
                                              String triggeredBy, int maxRetries) {
        JobExecutionRecord record = JobExecutionRecord.builder()
                .jobName(jobName)
                .jobGroup(jobGroup)
                .description(description)
                .status(JobStatus.RUNNING)
                .startedAt(Instant.now())
                .triggeredBy(triggeredBy)
                .success(false)
                .retryCount(0)
                .maxRetries(maxRetries)
                .nodeId(getNodeId())
                .build();

        JobExecutionRecord saved = recordRepository.save(record);
        log.info("Job execution started: [{}] id={}", jobName, saved.getId());
        return saved;
    }

    public void completeExecution(String recordId, boolean success, String resultMessage) {
        recordRepository.findById(recordId).ifPresent(record -> {
            Instant now = Instant.now();
            record.setStatus(success ? JobStatus.COMPLETED : JobStatus.FAILED);
            record.setCompletedAt(now);
            record.setSuccess(success);
            if (record.getStartedAt() != null) {
                record.setDurationMs(Duration.between(record.getStartedAt(), now).toMillis());
            }
            if (resultMessage != null && !success) {
                record.setErrorMessage(resultMessage);
            }
            recordRepository.save(record);
            log.info("Job execution [{}]: {} ({}ms)", record.getJobName(),
                    success ? "COMPLETED" : "FAILED", record.getDurationMs());
        });
    }

    public void failExecution(String recordId, String errorMessage, String stackTrace) {
        recordRepository.findById(recordId).ifPresent(record -> {
            Instant now = Instant.now();
            record.setStatus(JobStatus.FAILED);
            record.setCompletedAt(now);
            record.setSuccess(false);
            record.setErrorMessage(errorMessage);
            record.setStackTrace(stackTrace);
            if (record.getStartedAt() != null) {
                record.setDurationMs(Duration.between(record.getStartedAt(), now).toMillis());
            }
            recordRepository.save(record);
            log.error("Job execution failed: [{}] id={} error={}", record.getJobName(), recordId, errorMessage);
        });
    }

    public void updateRetryCount(String recordId, int retryCount) {
        recordRepository.findById(recordId).ifPresent(record -> {
            record.setRetryCount(retryCount);
            record.setStatus(JobStatus.RUNNING);
            recordRepository.save(record);
        });
    }

    public Page<JobExecutionRecord> getExecutionHistory(String jobName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return recordRepository.findByJobNameOrderByStartedAtDesc(jobName, pageable);
    }

    public Page<JobExecutionRecord> getRecentExecutions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        return recordRepository.findAll(pageable);
    }

    public List<JobExecutionRecord> getStuckExecutions(Instant threshold) {
        return recordRepository.findByStatusAndStartedAtBefore(JobStatus.RUNNING, threshold);
    }

    public List<JobExecutionRecord> getPendingRetries(String jobName) {
        return recordRepository.findByJobNameAndStatus(jobName, JobStatus.FAILED);
    }

    public Map<String, Object> getJobStatistics(String jobName, Instant since) {
        Map<String, Object> stats = new HashMap<>();
        long totalRuns = recordRepository.countByJobNameAndStartedAtAfter(jobName, since);
        long failures = recordRepository.countByJobNameAndSuccessAndStartedAtAfter(jobName, false, since);
        long successes = totalRuns - failures;

        stats.put("jobName", jobName);
        stats.put("totalRuns", totalRuns);
        stats.put("successes", successes);
        stats.put("failures", failures);
        stats.put("successRate", totalRuns > 0 ? (double) successes / totalRuns : 1.0);
        stats.put("since", since);

        return stats;
    }

    public List<JobExecutionRecordRepository.JobSummary> getAllJobSummaries() {
        return recordRepository.getJobSummaries();
    }

    private String getNodeId() {
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank()) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "unknown-" + Instant.now().toEpochMilli();
            }
        }
        return host;
    }
}
