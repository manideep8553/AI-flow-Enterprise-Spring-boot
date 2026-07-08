package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.impl.DocumentLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DocumentLifecycleJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(DocumentLifecycleJob.class);

    private final DocumentLifecycleService lifecycleService;

    public DocumentLifecycleJob(DistributedLockService distributedLockService,
                                 JobMonitorService jobMonitorService,
                                 DocumentLifecycleService lifecycleService) {
        super(distributedLockService, jobMonitorService);
        this.lifecycleService = lifecycleService;
    }

    @Override
    public String getJobName() {
        return "document-lifecycle";
    }

    @Override
    public String getJobGroup() {
        return "documents";
    }

    @Override
    public String getDescription() {
        return "Enforces document lifecycle policies — deletes expired documents, transitions storage classes";
    }

    @Override
    public int getLockTtlSeconds() {
        return 600;
    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.document-lifecycle.cron:0 0 2 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        int processed = lifecycleService.enforceLifecyclePolicies();
        log.info("Document lifecycle job processed {} documents", processed);
    }
}
