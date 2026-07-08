package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.impl.DocumentStorageOptimizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentStorageOptimizationJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageOptimizationJob.class);

    private final DocumentStorageOptimizer storageOptimizer;

    public DocumentStorageOptimizationJob(DistributedLockService distributedLockService,
                                           JobMonitorService jobMonitorService,
                                           DocumentStorageOptimizer storageOptimizer) {
        super(distributedLockService, jobMonitorService);
        this.storageOptimizer = storageOptimizer;
    }

    @Override
    public String getJobName() {
        return "document-storage-optimization";
    }

    @Override
    public String getJobGroup() {
        return "documents";
    }

    @Override
    public String getDescription() {
        return "Analyzes document access patterns and recommends or applies storage class transitions";
    }

    @Override
    public int getLockTtlSeconds() {
        return 900;
    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.document-storage-optimization.cron:0 0 4 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        int optimized = storageOptimizer.optimizeStorage();
        log.info("Storage optimization job completed: {} documents updated", optimized);
    }
}
