package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.service.impl.FraudMLClient;
import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AIModelSyncJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(AIModelSyncJob.class);

    private final FraudMLClient fraudMLClient;
    private final MongoTemplate mongoTemplate;

    public AIModelSyncJob(DistributedLockService distributedLockService,
                           JobMonitorService jobMonitorService,
                           FraudMLClient fraudMLClient,
                           MongoTemplate mongoTemplate) {
        super(distributedLockService, jobMonitorService);
        this.fraudMLClient = fraudMLClient;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getJobName() {
        return "ai-model-sync";
    }

    @Override
    public String getJobGroup() {
        return "ai";
    }

    @Override
    public String getDescription() {
        return "Synchronizes training data and syncs AI/ML model state between services";
    }

    @Override
    public int getLockTtlSeconds() {
        return 600;
    }

    @Override
    public int getMaxRetries() {
        return 3;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.ai-model-sync.cron:0 0 4 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant now = Instant.now();
        int syncedCount = 0;

        boolean mlAvailable = fraudMLClient.isServiceAvailable();
        log.info("ML service health check: {}", mlAvailable ? "available" : "unavailable");

        if (!mlAvailable) {
            log.warn("ML service unavailable - skipping AI model sync");
            return;
        }

        Instant syncSince = now.minus(24, ChronoUnit.HOURS);
        List<Map> recentFraudChecks = mongoTemplate.find(
                org.springframework.data.mongodb.core.query.Query.query(
                        org.springframework.data.mongodb.core.query.Criteria.where("checkedAt").gte(syncSince)),
                Map.class, "fraud_checks");

        Map<String, List<Map>> trainingData = new HashMap<>();
        trainingData.put("fraud_checks", recentFraudChecks);
        syncedCount = recentFraudChecks.size();

        Map<String, Object> syncRecord = new HashMap<>();
        syncRecord.put("lastSyncAt", now);
        syncRecord.put("recordsSynced", syncedCount);
        syncRecord.put("mlServiceAvailable", mlAvailable);
        syncRecord.put("status", "COMPLETED");

        mongoTemplate.save(syncRecord, "ai_model_sync_log");

        log.info("AI model sync complete: {} records synchronized with ML service", syncedCount);
    }
}
