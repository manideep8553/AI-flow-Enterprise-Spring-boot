package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class EscalationPolicyJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(EscalationPolicyJob.class);

    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    public EscalationPolicyJob(DistributedLockService distributedLockService,
                                JobMonitorService jobMonitorService,
                                MongoTemplate mongoTemplate,
                                EmailService emailService) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
        this.emailService = emailService;
    }

    @Override
    public String getJobName() {
        return "escalation-policy-enforcer";
    }

    @Override
    public String getJobGroup() {
        return "governance";
    }

    @Override
    public String getDescription() {
        return "Enforces escalation policies for overdue approvals, stalled requests, and aging tasks";
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
    @Scheduled(cron = "${app.scheduler.jobs.escalation-policy.cron:0 */30 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void execute() {
        Instant now = Instant.now();
        int escalated = 0;

        List<Map> stalledRequests = mongoTemplate.find(
                Query.query(Criteria.where("status").in("SUBMITTED", "PENDING_APPROVAL", "IN_PROGRESS")),
                Map.class, "requests");

        for (Map<String, Object> request : stalledRequests) {
            try {
                Instant updatedAt = request.get("updatedAt") instanceof Instant
                        ? (Instant) request.get("updatedAt") : null;
                Instant createdAt = request.get("createdAt") instanceof Instant
                        ? (Instant) request.get("createdAt") : null;
                Instant stalledSince = updatedAt != null ? updatedAt : createdAt;
                if (stalledSince == null) continue;

                long hoursStalled = ChronoUnit.HOURS.between(stalledSince, now);
                String requestId = request.get("_id") != null ? request.get("_id").toString() : "";

                int currentLevel = request.get("escalationLevel") instanceof Number
                        ? ((Number) request.get("escalationLevel")).intValue() : 0;

                if (hoursStalled > 24 && currentLevel < 3) {
                    int newLevel = currentLevel + 1;
                    mongoTemplate.findAndModify(
                            Query.query(Criteria.where("_id").is(requestId)),
                            new Update().set("escalationLevel", newLevel)
                                    .set("escalated", true)
                                    .set("escalatedAt", now),
                            Map.class, "requests");

                    String currentApprover = (String) request.get("currentApprover");
                    if (currentApprover != null) {
                        Query userQuery = Query.query(Criteria.where("username").is(currentApprover));
                        Map user = mongoTemplate.findOne(userQuery, Map.class, "users");
                        if (user != null) {
                            emailService.sendSimpleEmail((String) user.get("email"),
                                    "Escalation Alert: Request " + requestId,
                                    "Request " + requestId + " has been escalated to level " + newLevel
                                            + " (" + hoursStalled + " hours stalled).");
                        }
                    }
                    escalated++;
                    log.info("Escalated request {} to level {}", requestId, newLevel);
                }
            } catch (Exception e) {
                log.warn("Failed to process escalation for request {}: {}", request.get("_id"), e.getMessage());
            }
        }

        List<Map> overdueTasks = mongoTemplate.find(
                Query.query(Criteria.where("status").in("PENDING", "IN_PROGRESS").and("dueDate").lt(now)),
                Map.class, "tasks");

        for (Map<String, Object> task : overdueTasks) {
            try {
                String taskId = task.get("_id") != null ? task.get("_id").toString() : "";
                Object escalationObj = task.get("escalationLevel");
                int level = escalationObj instanceof Number ? ((Number) escalationObj).intValue() : 0;

                if (level < 3) {
                    mongoTemplate.findAndModify(
                            Query.query(Criteria.where("_id").is(taskId)),
                            new Update().inc("escalationLevel", 1),
                            Map.class, "tasks");
                    escalated++;
                }
            } catch (Exception e) {
                log.warn("Failed to escalate task {}: {}", task.get("_id"), e.getMessage());
            }
        }

        log.info("Escalation policy enforced: {} escalations triggered", escalated);
    }
}
