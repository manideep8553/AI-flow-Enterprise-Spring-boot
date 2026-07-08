package com.aiflow.enterprise.scheduler.config;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    private static final Logger log = LoggerFactory.getLogger(SchedulerConfig.class);

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(throwable -> {
            log.error("Unhandled error in scheduled task: {}", throwable.getMessage(), throwable);
        });
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public Executor jobExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    @ConditionalOnProperty(name = "app.scheduler.stuck-job-monitor.enabled", havingValue = "true", matchIfMissing = true)
    public StuckJobMonitor stuckJobMonitor(JobMonitorService jobMonitorService,
                                            DistributedLockService distributedLockService) {
        return new StuckJobMonitor(jobMonitorService, distributedLockService);
    }

    static class StuckJobMonitor {

        private static final Logger log = LoggerFactory.getLogger(StuckJobMonitor.class);

        private final JobMonitorService jobMonitorService;
        private final DistributedLockService distributedLockService;

        StuckJobMonitor(JobMonitorService jobMonitorService,
                        DistributedLockService distributedLockService) {
            this.jobMonitorService = jobMonitorService;
            this.distributedLockService = distributedLockService;
        }

        @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.scheduler.stuck-job-monitor.interval:300000}")
        public void monitorStuckJobs() {
            String lockKey = "stuck-job-monitor-lock";
            if (!distributedLockService.acquireLock(lockKey, 120)) {
                return;
            }
            try {
                Instant threshold = Instant.now().minusSeconds(3600);
                var stuckJobs = jobMonitorService.getStuckExecutions(threshold);
                for (var job : stuckJobs) {
                    log.warn("Found stuck job execution: id={} name={} started={}",
                            job.getId(), job.getJobName(), job.getStartedAt());
                    jobMonitorService.failExecution(job.getId(),
                            "Execution timed out - stuck for > 1 hour", null);
                }
                if (!stuckJobs.isEmpty()) {
                    log.info("Marked {} stuck jobs as failed", stuckJobs.size());
                }
            } finally {
                distributedLockService.releaseLock(lockKey);
            }
        }
    }

    @Bean
    public CronTrigger cronTrigger(String expression) {
        return new CronTrigger(expression);
    }
}
