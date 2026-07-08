package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class RecurringApprovalJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(RecurringApprovalJob.class);

    private final MongoTemplate mongoTemplate;

    public RecurringApprovalJob(DistributedLockService distributedLockService,
                                 JobMonitorService jobMonitorService,
                                 MongoTemplate mongoTemplate) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getJobName() {
        return "recurring-approval-processor";
    }

    @Override
    public String getJobGroup() {
        return "approval";
    }

    @Override
    public String getDescription() {
        return "Processes recurring approval requests and auto-approves based on configured rules";
    }

    @Override
    public int getLockTtlSeconds() {
        return 180;
    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.recurring-approval.cron:0 */15 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void execute() {
        Instant now = Instant.now();
        int autoApproved = 0;
        int escalated = 0;

        Query query = Query.query(Criteria.where("metadata.recurring").is(true)
                .and("status").in("SUBMITTED", "PENDING_APPROVAL"));
        List<Map> recurringRequests = mongoTemplate.find(query, Map.class, "requests");

        for (Map<String, Object> request : recurringRequests) {
            try {
                String requestId = request.get("_id") != null ? request.get("_id").toString() : null;
                if (requestId == null) continue;

                boolean canAutoApprove = canAutoApprove(request);
                if (canAutoApprove) {
                    mongoTemplate.findAndModify(
                            Query.query(Criteria.where("_id").is(requestId)),
                            new Update().set("status", "APPROVED").set("updatedAt", now),
                            Map.class, "requests");
                    autoApproved++;
                    log.info("Auto-approved recurring request: id={}", requestId);
                } else {
                    mongoTemplate.findAndModify(
                            Query.query(Criteria.where("_id").is(requestId)),
                            new Update().set("status", "IN_PROGRESS").set("updatedAt", now),
                            Map.class, "requests");
                    escalated++;
                }
            } catch (Exception e) {
                log.warn("Failed to process recurring request {}: {}", request.get("_id"), e.getMessage());
            }
        }

        log.info("Recurring approvals processed: {} auto-approved, {} escalated", autoApproved, escalated);
    }

    @SuppressWarnings("unchecked")
    private boolean canAutoApprove(Map<String, Object> request) {
        Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");
        if (metadata == null) return false;

        Object maxAmountObj = metadata.get("recurringMaxAmount");
        double maxAmount = maxAmountObj instanceof Number ? ((Number) maxAmountObj).doubleValue() : 5000;

        Object amountObj = request.get("fields") instanceof Map
                ? ((Map<String, Object>) request.get("fields")).get("amount") : null;
        double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;

        return amount <= maxAmount;
    }
}
