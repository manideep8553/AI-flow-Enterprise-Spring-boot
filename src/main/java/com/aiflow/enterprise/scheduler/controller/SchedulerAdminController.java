package com.aiflow.enterprise.scheduler.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.scheduler.job.AbstractScheduledJob;
import com.aiflow.enterprise.scheduler.model.JobExecutionRecord;
import com.aiflow.enterprise.scheduler.repository.JobExecutionRecordRepository;
import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/scheduler")
@Tag(name = "Scheduler Admin", description = "Admin endpoints for managing and monitoring scheduled jobs")
public class SchedulerAdminController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerAdminController.class);

    private final JobMonitorService jobMonitorService;
    private final DistributedLockService distributedLockService;
    private final List<AbstractScheduledJob> scheduledJobs;
    private final JobLauncher batchJobLauncher;
    private final List<Job> batchJobs;

    public SchedulerAdminController(JobMonitorService jobMonitorService,
                                     DistributedLockService distributedLockService,
                                     List<AbstractScheduledJob> scheduledJobs,
                                     JobLauncher batchJobLauncher,
                                     List<Job> batchJobs) {
        this.jobMonitorService = jobMonitorService;
        this.distributedLockService = distributedLockService;
        this.scheduledJobs = scheduledJobs;
        this.batchJobLauncher = batchJobLauncher;
        this.batchJobs = batchJobs;
    }

    @GetMapping("/jobs")
    @Operation(summary = "List all registered scheduled jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listJobs() {
        List<Map<String, Object>> jobList = scheduledJobs.stream()
                .map(job -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("jobName", job.getJobName());
                    info.put("jobGroup", job.getJobGroup());
                    info.put("description", job.getDescription());
                    info.put("lockTtlSeconds", job.getLockTtlSeconds());
                    info.put("maxRetries", job.getMaxRetries());
                    info.put("locked", distributedLockService.isLocked("job-lock:" + job.getJobName()));
                    return info;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(jobList));
    }

    @GetMapping("/batch-jobs")
    @Operation(summary = "List all registered batch jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> listBatchJobs() {
        List<String> jobNames = batchJobs.stream()
                .map(Job::getName)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(jobNames));
    }

    @PostMapping("/jobs/{jobName}/trigger")
    @Operation(summary = "Manually trigger a scheduled job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerJob(@PathVariable String jobName) {
        AbstractScheduledJob target = scheduledJobs.stream()
                .filter(j -> j.getJobName().equals(jobName))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Job not found: " + jobName, HttpStatus.BAD_REQUEST));
        }

        target.runManual("ADMIN_API");
        log.info("Manual trigger initiated for job: {}", jobName);
        return ResponseEntity.ok(ApiResponse.success("Job '" + jobName + "' triggered manually"));
    }

    @PostMapping("/batch-jobs/{jobName}/launch")
    @Operation(summary = "Launch a batch job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> launchBatchJob(@PathVariable String jobName) {
        Job target = batchJobs.stream()
                .filter(j -> j.getName().equals(jobName))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Batch job not found: " + jobName, HttpStatus.BAD_REQUEST));
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("triggeredBy", "ADMIN_API")
                    .addString("triggeredAt", Instant.now().toString())
                    .toJobParameters();
            batchJobLauncher.run(target, params);
            return ResponseEntity.ok(ApiResponse.success("Batch job '" + jobName + "' launched"));
        } catch (Exception e) {
            log.error("Failed to launch batch job '{}': {}", jobName, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to launch batch job: " + e.getMessage(),
                            HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/executions")
    @Operation(summary = "Get recent job execution history")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<JobExecutionRecord>>> getExecutionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<JobExecutionRecord> records = jobMonitorService.getRecentExecutions(page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/executions/{jobName}")
    @Operation(summary = "Get execution history for a specific job")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<JobExecutionRecord>>> getJobExecutions(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<JobExecutionRecord> records = jobMonitorService.getExecutionHistory(jobName, page, size);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/executions/{jobName}/statistics")
    @Operation(summary = "Get job execution statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobStatistics(
            @PathVariable String jobName,
            @RequestParam(defaultValue = "24h") String timeframe) {
        Instant since;
        since = switch (timeframe) {
            case "24h" -> Instant.now().minusSeconds(86400);
            case "7d" -> Instant.now().minusSeconds(604800);
            case "30d" -> Instant.now().minusSeconds(2592000);
            default -> Instant.now().minusSeconds(86400);
        };

        Map<String, Object> stats = jobMonitorService.getJobStatistics(jobName, since);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get summary of all jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<JobExecutionRecordRepository.JobSummary>>> getSummary() {
        var summaries = jobMonitorService.getAllJobSummaries();
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    @GetMapping("/locks")
    @Operation(summary = "List all active distributed locks")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getActiveLocks() {
        long count = distributedLockService.getActiveLockCount();
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/locks/{lockKey}")
    @Operation(summary = "Release a distributed lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> releaseLock(@PathVariable String lockKey) {
        distributedLockService.releaseLock(lockKey);
        return ResponseEntity.ok(ApiResponse.success("Lock released: " + lockKey));
    }

    @GetMapping("/health")
    @Operation(summary = "Scheduler system health check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("activeLocks", distributedLockService.getActiveLockCount());
        health.put("registeredJobs", scheduledJobs.size());
        health.put("registeredBatchJobs", batchJobs.size());
        health.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
