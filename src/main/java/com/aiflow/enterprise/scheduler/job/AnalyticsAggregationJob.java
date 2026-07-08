package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AnalyticsAggregationJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationJob.class);

    private final MongoTemplate mongoTemplate;

    public AnalyticsAggregationJob(DistributedLockService distributedLockService,
                                    JobMonitorService jobMonitorService,
                                    MongoTemplate mongoTemplate) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getJobName() {
        return "analytics-aggregation";
    }

    @Override
    public String getJobGroup() {
        return "analytics";
    }

    @Override
    public String getDescription() {
        return "Aggregates platform analytics data: requests, workflows, users, and audit activity";
    }

    @Override
    public int getLockTtlSeconds() {
        return 300;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.analytics-aggregation.cron:0 0 */6 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(24, ChronoUnit.HOURS);
        Instant lastWeek = now.minus(7, ChronoUnit.DAYS);
        Instant lastMonth = now.minus(30, ChronoUnit.DAYS);

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("aggregatedAt", now);
        analytics.put("periodStart", yesterday);
        analytics.put("periodEnd", now);

        try {
            long totalUsers = mongoTemplate.count(new Query(), "users");
            Query activeUsersQuery = Query.query(Criteria.where("lastLoginAt").gte(yesterday));
            long activeUsers = mongoTemplate.count(activeUsersQuery, "users");
            analytics.put("totalUsers", totalUsers);
            analytics.put("activeUsersLast24h", activeUsers);

            long requestsLast24h = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(yesterday)), "requests");
            long requestsLast7d = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(lastWeek)), "requests");
            long requestsLast30d = mongoTemplate.count(
                    Query.query(Criteria.where("createdAt").gte(lastMonth)), "requests");
            analytics.put("requestsLast24h", requestsLast24h);
            analytics.put("requestsLast7d", requestsLast7d);
            analytics.put("requestsLast30d", requestsLast30d);

            long workflowsExecuted = mongoTemplate.count(
                    Query.query(Criteria.where("startedAt").gte(yesterday)), "workflow_executions");
            long workflowsSucceeded = mongoTemplate.count(
                    Query.query(Criteria.where("status").is("COMPLETED")
                            .and("startedAt").gte(yesterday)), "workflow_executions");
            analytics.put("workflowsExecuted24h", workflowsExecuted);
            analytics.put("workflowsSucceeded24h", workflowsSucceeded);

            long auditEvents = mongoTemplate.count(
                    Query.query(Criteria.where("timestamp").gte(yesterday).lt(now)), "audit_logs");
            analytics.put("auditEvents24h", auditEvents);

            List<Map> recentExecs = mongoTemplate.find(
                    Query.query(Criteria.where("startedAt").gte(yesterday)
                            .and("completedAt").exists(true)),
                    Map.class, "workflow_executions");

            double avgDuration = 0;
            if (!recentExecs.isEmpty()) {
                avgDuration = recentExecs.stream()
                        .filter(e -> e.get("startedAt") instanceof Instant && e.get("completedAt") instanceof Instant)
                        .mapToLong(e -> ChronoUnit.SECONDS.between(
                                (Instant) e.get("startedAt"), (Instant) e.get("completedAt")))
                        .average()
                        .orElse(0);
            }
            analytics.put("avgWorkflowDurationSeconds", avgDuration);

            Map<String, Long> statusBreakdown = new HashMap<>();
            for (String status : List.of("DRAFT", "SUBMITTED", "PENDING_APPROVAL", "APPROVED", "REJECTED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "ESCALATED")) {
                long count = mongoTemplate.count(
                        Query.query(Criteria.where("status").is(status)), "requests");
                if (count > 0) {
                    statusBreakdown.put(status, count);
                }
            }
            analytics.put("requestStatusBreakdown", statusBreakdown);

            mongoTemplate.save(analytics, "analytics_snapshots");
            log.info("Analytics aggregated: {} users, {} requests/24h, {} workflows/24h",
                    totalUsers, requestsLast24h, workflowsExecuted);

        } catch (Exception e) {
            log.error("Failed to aggregate analytics: {}", e.getMessage(), e);
            throw e;
        }
    }
}
