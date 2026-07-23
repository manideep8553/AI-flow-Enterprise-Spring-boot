package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.response.AnalyticsSummaryResponse;
import com.aiflow.enterprise.dto.response.AnalyticsSummaryResponse.*;
import com.aiflow.enterprise.enums.*;
import com.aiflow.enterprise.notification.repository.NotificationRepository;
import com.aiflow.enterprise.repository.*;
import com.aiflow.enterprise.scheduler.repository.JobExecutionRecordRepository;
import com.aiflow.enterprise.scheduler.repository.DistributedLockRepository;
import com.aiflow.enterprise.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final MongoTemplate mongoTemplate;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final RequestRepository requestRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final FraudCheckRepository fraudCheckRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final AIDecisionLogRepository aiDecisionLogRepository;
    private final NotificationRepository notificationRepository;
    private final JobExecutionRecordRepository jobExecutionRecordRepository;
    private final DistributedLockRepository distributedLockRepository;

    public AnalyticsServiceImpl(MongoTemplate mongoTemplate,
                                 WorkflowExecutionRepository workflowExecutionRepository,
                                 RequestRepository requestRepository,
                                 DocumentRepository documentRepository,
                                 UserRepository userRepository,
                                 UserSessionRepository userSessionRepository,
                                 AuditLogRepository auditLogRepository,
                                 FraudCheckRepository fraudCheckRepository,
                                 FraudAlertRepository fraudAlertRepository,
                                 AIDecisionLogRepository aiDecisionLogRepository,
                                 NotificationRepository notificationRepository,
                                 JobExecutionRecordRepository jobExecutionRecordRepository,
                                 DistributedLockRepository distributedLockRepository) {
        this.mongoTemplate = mongoTemplate;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.requestRepository = requestRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.fraudCheckRepository = fraudCheckRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.aiDecisionLogRepository = aiDecisionLogRepository;
        this.notificationRepository = notificationRepository;
        this.jobExecutionRecordRepository = jobExecutionRecordRepository;
        this.distributedLockRepository = distributedLockRepository;
    }

    private Instant periodStart(String period) {
        Instant now = Instant.now();
        if (period == null) return now.minus(30, ChronoUnit.DAYS);
        return switch (period.toLowerCase()) {
            case "24h" -> now.minus(24, ChronoUnit.HOURS);
            case "7d" -> now.minus(7, ChronoUnit.DAYS);
            case "30d" -> now.minus(30, ChronoUnit.DAYS);
            case "90d" -> now.minus(90, ChronoUnit.DAYS);
            case "1y" -> now.minus(365, ChronoUnit.DAYS);
            default -> now.minus(30, ChronoUnit.DAYS);
        };
    }

    private long periodDurationMillis(String period) {
        return switch (period != null ? period.toLowerCase() : "30d") {
            case "24h" -> 86400000L;
            case "7d" -> 604800000L;
            case "30d" -> 2592000000L;
            case "90d" -> 7776000000L;
            case "1y" -> 31536000000L;
            default -> 2592000000L;
        };
    }

    @Override
    @Cacheable(value = "analytics-summary", key = "#period + '-' + #department + '-' + #workflowId", unless = "#result == null")
    public AnalyticsSummaryResponse getSummary(String period, String department, String workflowId) {
        Instant since = periodStart(period);
        Instant now = Instant.now();

        return AnalyticsSummaryResponse.builder()
                .kpi(buildKPIMetrics(since, now))
                .workflows(buildWorkflowMetrics(since, now))
                .requests(buildRequestMetrics(since, now))
                .users(buildUserMetrics(since, now))
                .documents(buildDocumentMetrics())
                .fraud(buildFraudMetrics(since, now))
                .notifications(buildNotificationMetrics(since, now))
                .ai(buildAIMetrics(since, now))
                .scheduler(buildSchedulerMetrics())
                .bottlenecks(buildBottlenecks())
                .departments(buildDepartmentMetrics(since, now))
                .generatedAt(now)
                .period(period != null ? period : "30d")
                .build();
    }

    @Override
    @Cacheable(value = "analytics-workflows", key = "#period", unless = "#result == null")
    public AnalyticsSummaryResponse getWorkflowAnalytics(String period) {
        Instant since = periodStart(period);
        return AnalyticsSummaryResponse.builder()
                .workflows(buildWorkflowMetrics(since, Instant.now()))
                .generatedAt(Instant.now())
                .period(period != null ? period : "30d")
                .build();
    }

    @Override
    @Cacheable(value = "analytics-workflow-metrics", key = "#period", unless = "#result == null")
    public WorkflowMetrics getWorkflowMetrics(String period) {
        return buildWorkflowMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-request-metrics", key = "#period", unless = "#result == null")
    public RequestMetrics getRequestMetrics(String period) {
        return buildRequestMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-user-metrics", key = "#period", unless = "#result == null")
    public UserMetrics getUserMetrics(String period) {
        return buildUserMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-document-metrics", unless = "#result == null")
    public DocumentMetrics getDocumentMetrics() {
        return buildDocumentMetrics();
    }

    @Override
    @Cacheable(value = "analytics-fraud-metrics", key = "#period", unless = "#result == null")
    public FraudMetrics getFraudMetrics(String period) {
        return buildFraudMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-notification-metrics", key = "#period", unless = "#result == null")
    public NotificationMetrics getNotificationMetrics(String period) {
        return buildNotificationMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-ai-metrics", key = "#period", unless = "#result == null")
    public AIMetrics getAIMetrics(String period) {
        return buildAIMetrics(periodStart(period), Instant.now());
    }

    @Override
    @Cacheable(value = "analytics-scheduler-metrics", unless = "#result == null")
    public SchedulerMetrics getSchedulerMetrics() {
        return buildSchedulerMetrics();
    }

    @Override
    @Cacheable(value = "analytics-bottlenecks", unless = "#result == null")
    public List<BottleneckItem> getBottlenecks() {
        return buildBottlenecks();
    }

    @Override
    @Cacheable(value = "analytics-department-metrics", key = "#period", unless = "#result == null")
    public List<DepartmentMetric> getDepartmentMetrics(String period) {
        return buildDepartmentMetrics(periodStart(period), Instant.now());
    }

    // -- KPI --

    private KPIMetrics buildKPIMetrics(Instant since, Instant now) {
        long totalExecs = mongoTemplate.count(new Query(), "workflow_executions");
        long completedExecs = countByField("workflow_executions", "status", "COMPLETED");
        long failedExecs = countByField("workflow_executions", "status", "FAILED");
        double completionRate = totalExecs > 0 ? (double) completedExecs / totalExecs * 100 : 0;

        double avgDuration = computeAvgDurationMs(since, now) / 1000.0;
        long prevStart = since.toEpochMilli() - (now.toEpochMilli() - since.toEpochMilli());
        double prevAvgDuration = computeAvgDurationMs(
                Instant.ofEpochMilli(prevStart), since) / 1000.0;
        double avgDurationChange = prevAvgDuration > 0
                ? ((avgDuration - prevAvgDuration) / prevAvgDuration) * 100 : 0;

        long totalUsers = mongoTemplate.count(new Query(), "users");
        long activeUsers = mongoTemplate.count(
                Query.query(Criteria.where("lastLoginAt").gte(since)), "users");
        long prevActive = mongoTemplate.count(
                Query.query(Criteria.where("lastLoginAt").gte(Instant.ofEpochMilli(prevStart))
                        .lt(since)), "users");
        double activeUserChange = prevActive > 0
                ? ((double) (activeUsers - prevActive) / prevActive) * 100 : 0;

        long totalRequests = mongoTemplate.count(new Query(), "requests");
        long pendingApprovals = countByField("requests", "status", "PENDING_APPROVAL");
        long prevReqs = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").lt(since)), "requests");
        double reqChange = prevReqs > 0
                ? ((double) (totalRequests - prevReqs) / prevReqs) * 100 : 0;

        long totalDocs = mongoTemplate.count(new Query(), "documents");
        long totalStorage = aggregateSum("documents", "fileSize");
        long openAlerts = mongoTemplate.count(
                Query.query(Criteria.where("resolved").ne(true)), "fraud_alerts");
        long unreadNotifs = mongoTemplate.count(
                Query.query(Criteria.where("read").ne(true)), "notifications");

        return KPIMetrics.builder()
                .totalExecutions(totalExecs)
                .completedExecutions(completedExecs)
                .failedExecutions(failedExecs)
                .completionRate(round1(completionRate))
                .avgDurationSeconds(round1(avgDuration))
                .avgDurationChange(round1(avgDurationChange))
                .activeUsers(activeUsers)
                .totalUsers(totalUsers)
                .activeUserChange(round1(activeUserChange))
                .totalRequests(totalRequests)
                .pendingApprovals(pendingApprovals)
                .requestChange(round1(reqChange))
                .totalDocuments(totalDocs)
                .totalStorageBytes(totalStorage)
                .openFraudAlerts(openAlerts)
                .unreadNotifications(unreadNotifs)
                .build();
    }

    // -- Workflow Metrics --

    private WorkflowMetrics buildWorkflowMetrics(Instant since, Instant now) {
        long total = mongoTemplate.count(new Query(), "workflow_executions");
        long running = countByField("workflow_executions", "status", "RUNNING");
        long completed = countByField("workflow_executions", "status", "COMPLETED");
        long failed = countByField("workflow_executions", "status", "FAILED");
        long cancelled = countByField("workflow_executions", "status", "CANCELLED");
        long suspended = countByField("workflow_executions", "status", "SUSPENDED");
        double successRate = total > 0 ? (double) completed / total * 100 : 0;

        return WorkflowMetrics.builder()
                .total(total)
                .running(running)
                .completed(completed)
                .failed(failed)
                .cancelled(cancelled)
                .suspended(suspended)
                .successRate(round1(successRate))
                .avgDurationSeconds(round1(computeAvgDurationMs(since, now) / 1000.0))
                .totalThisPeriod(mongoTemplate.count(
                        Query.query(Criteria.where("createdAt").gte(since)), "workflow_executions"))
                .trend(computeDailyTrend("workflow_executions", "createdAt", since, now))
                .byWorkflow(groupByStringField("workflow_executions", "workflowName", since, now))
                .byTriggerType(groupByStringField("workflow_executions", "triggerType", since, now))
                .build();
    }

    // -- Request Metrics --

    private RequestMetrics buildRequestMetrics(Instant since, Instant now) {
        long total = mongoTemplate.count(new Query(), "requests");
        long submitted = countByField("requests", "status", "SUBMITTED");
        long pendingApproval = countByField("requests", "status", "PENDING_APPROVAL");
        long approved = countByField("requests", "status", "APPROVED");
        long rejected = countByField("requests", "status", "REJECTED");
        long cancelled = countByField("requests", "status", "CANCELLED");
        double approvalRate = (approved + rejected) > 0
                ? (double) approved / (approved + rejected) * 100 : 0;

        return RequestMetrics.builder()
                .total(total)
                .submitted(submitted)
                .pendingApproval(pendingApproval)
                .approved(approved)
                .rejected(rejected)
                .cancelled(cancelled)
                .approvalRate(round1(approvalRate))
                .avgApprovalTimeHours(round1(computeAvgApprovalTimeHours(since, now)))
                .slaCompliance(round1(computeSLACompliance(since, now)))
                .escalated(mongoTemplate.count(
                        Query.query(Criteria.where("escalated").is(true)), "requests"))
                .byType(groupByStringField("requests", "requestTypeName", since, now))
                .byPriority(groupByStringField("requests", "priority", since, now))
                .trend(computeDailyTrend("requests", "createdAt", since, now))
                .approvalTrend(computeApprovalTrend(since, now))
                .build();
    }

    // -- User Metrics --

    private UserMetrics buildUserMetrics(Instant since, Instant now) {
        long total = mongoTemplate.count(new Query(), "users");
        long active = mongoTemplate.count(
                Query.query(Criteria.where("lastLoginAt").gte(since)), "users");
        long newThisPeriod = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(since)), "users");
        long withSessions = mongoTemplate.count(
                Query.query(Criteria.where("active").is(true)), "user_sessions");

        return UserMetrics.builder()
                .total(total)
                .active(active)
                .newThisPeriod(newThisPeriod)
                .withSessions(withSessions)
                .byRole(groupByStringField("users", "role", null, null))
                .loginTrend(computeLoginTrend(since, now))
                .topActive(getTopActiveUsers(since))
                .build();
    }

    // -- Document Metrics --

    private DocumentMetrics buildDocumentMetrics() {
        return DocumentMetrics.builder()
                .total(mongoTemplate.count(new Query(), "documents"))
                .processing(countByField("documents", "processingStatus", "PROCESSING"))
                .completed(countByField("documents", "processingStatus", "COMPLETED"))
                .failed(countByField("documents", "processingStatus", "FAILED"))
                .archived(mongoTemplate.count(
                        Query.query(Criteria.where("archived").is(true)), "documents"))
                .totalStorageBytes(aggregateSum("documents", "fileSize"))
                .avgFileSizeBytes(aggregateAvg("documents", "fileSize"))
                .byType(groupAllStringField("documents", "documentType"))
                .byCategory(groupAllStringField("documents", "category"))
                .uploadTrend(computeDailyTrend("documents", "uploadedAt", 
                        Instant.now().minus(30, ChronoUnit.DAYS), Instant.now()))
                .build();
    }

    // -- Fraud Metrics --

    private FraudMetrics buildFraudMetrics(Instant since, Instant now) {
        return FraudMetrics.builder()
                .totalChecks(mongoTemplate.count(new Query(), "fraud_checks"))
                .confirmed(countByField("fraud_checks", "status", "CONFIRMED"))
                .dismissed(countByField("fraud_checks", "status", "DISMISSED"))
                .pendingReview(countByField("fraud_checks", "status", "PENDING_REVIEW"))
                .escalated(mongoTemplate.count(
                        Query.query(Criteria.where("escalated").is(true)), "fraud_checks"))
                .openAlerts(mongoTemplate.count(
                        Query.query(Criteria.where("resolved").ne(true)), "fraud_alerts"))
                .avgRiskScore(aggregateAvg("fraud_checks", "overallRiskScore"))
                .byRiskLevel(groupAllStringField("fraud_checks", "riskLevel"))
                .byDepartment(groupByStringField("fraud_checks", "department", since, now))
                .trend(computeFraudTrend(since, now))
                .build();
    }

    // -- Notification Metrics --

    private NotificationMetrics buildNotificationMetrics(Instant since, Instant now) {
        long sent = countByField("notifications", "status", "SENT");
        long delivered = countByField("notifications", "status", "DELIVERED");
        double deliveryRate = (sent + delivered) > 0
                ? (double) delivered / (sent + delivered) * 100 : 0;

        return NotificationMetrics.builder()
                .total(mongoTemplate.count(new Query(), "notifications"))
                .sent(sent)
                .delivered(delivered)
                .failed(countByField("notifications", "status", "FAILED"))
                .pending(countByField("notifications", "status", "PENDING"))
                .unread(mongoTemplate.count(
                        Query.query(Criteria.where("read").ne(true)), "notifications"))
                .deliveryRate(round1(deliveryRate))
                .byType(groupAllStringField("notifications", "type"))
                .byChannel(groupAllStringField("delivery_records", "channel"))
                .trend(computeDailyTrend("notifications", "createdAt", since, now))
                .build();
    }

    // -- AI Metrics --

    private AIMetrics buildAIMetrics(Instant since, Instant now) {
        Query periodQ = Query.query(Criteria.where("createdAt").gte(since));
        long totalCalls = mongoTemplate.count(periodQ, "ai_decision_logs");
        long accepted = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(since).and("accepted").is(true)),
                "ai_decision_logs");
        long feedbackProvided = mongoTemplate.count(
                Query.query(Criteria.where("createdAt").gte(since).and("feedbackProvided").is(true)),
                "ai_decision_logs");
        double acceptanceRate = totalCalls > 0 ? (double) accepted / totalCalls * 100 : 0;

        return AIMetrics.builder()
                .totalCalls(totalCalls)
                .accepted(accepted)
                .feedbackProvided(feedbackProvided)
                .acceptanceRate(round1(acceptanceRate))
                .avgResponseTimeMs(round1(aggregateAvg("ai_decision_logs", "responseTimeMs")))
                .totalTokensUsed(Math.round(aggregateSum("ai_decision_logs", "totalTokens")))
                .byRequestType(groupByStringField("ai_decision_logs", "requestType", since, now))
                .byConfidenceLevel(groupByStringField("ai_decision_logs", "confidenceLevel", since, now))
                .build();
    }

    // -- Scheduler Metrics --

    private SchedulerMetrics buildSchedulerMetrics() {
        Instant todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long completedToday = mongoTemplate.count(
                Query.query(Criteria.where("startedAt").gte(todayStart).and("success").is(true)),
                "job_execution_records");
        long failedToday = mongoTemplate.count(
                Query.query(Criteria.where("startedAt").gte(todayStart).and("success").is(false)),
                "job_execution_records");
        long totalToday = completedToday + failedToday;
        double successRate = totalToday > 0 ? (double) completedToday / totalToday * 100 : 100;

        List<JobHealthItem> jobs = new ArrayList<>();
        try {
            var summaries = jobExecutionRecordRepository.getJobSummaries();
            for (var s : summaries) {
                long totalRuns = s.getTotalRuns();
                long failures = s.getFailures();
                jobs.add(JobHealthItem.builder()
                        .jobName(s.getJobName())
                        .totalRuns(totalRuns)
                        .failures(failures)
                        .successRate(totalRuns > 0
                                ? round1((double) (totalRuns - failures) / totalRuns * 100) : 100)
                        .lastRun(s.getLastRun())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to load job summaries: {}", e.getMessage());
        }

        return SchedulerMetrics.builder()
                .totalJobs(mongoTemplate.count(new Query(), "job_execution_records"))
                .running(mongoTemplate.count(
                        Query.query(Criteria.where("status").is("RUNNING")), "job_execution_records"))
                .completedToday(completedToday)
                .failedToday(failedToday)
                .successRate(round1(successRate))
                .activeLocks(mongoTemplate.count(new Query(), "distributed_locks"))
                .jobs(jobs)
                .build();
    }

    // -- Bottlenecks --

    private List<BottleneckItem> buildBottlenecks() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("startedAt").gte(since).and("executionLog").exists(true)),
                    unwind("executionLog"),
                    match(Criteria.where("executionLog.status").in("FAILED", "TIMED_OUT")),
                    group("executionLog.stepId")
                            .count().as("occurrenceCount")
                            .addToSet("workflowName").as("workflowNames")
                            .addToSet("workflowId").as("workflowIds")
                            .addToSet("executionLog.stepName").as("stepNames"),
                    sort(Sort.Direction.DESC, "occurrenceCount"),
                    limit(20),
                    project("occurrenceCount", "workflowNames", "workflowIds", "stepNames")
                            .and("_id").as("stepId")
            );
            var results = mongoTemplate.aggregate(agg, "workflow_executions", Map.class);
            List<BottleneckItem> items = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                String stepId = str(r.get("stepId"));
                long count = longVal(r.get("occurrenceCount"));
                @SuppressWarnings("unchecked")
                List<String> wfNames = (List<String>) r.get("workflowNames");
                String wfName = (wfNames != null && !wfNames.isEmpty()) ? wfNames.get(0) : "Unknown";
                @SuppressWarnings("unchecked")
                List<String> wfIds = (List<String>) r.get("workflowIds");
                String wfId = (wfIds != null && !wfIds.isEmpty()) ? wfIds.get(0) : null;
                @SuppressWarnings("unchecked")
                List<String> stepNames = (List<String>) r.get("stepNames");
                String stepName = (stepNames != null && !stepNames.isEmpty()) ? stepNames.get(0) : stepId;

                double failureRate = computeStepFailureRate(stepId, since);
                double avgDuration = computeStepAvgDurationMs(stepId, since) / 1000.0;
                String severity = failureRate > 50 ? "HIGH" : failureRate > 20 ? "MEDIUM" : "LOW";

                items.add(BottleneckItem.builder()
                        .workflowId(wfId)
                        .workflowName(wfName)
                        .stepId(stepId)
                        .stepName(stepName)
                        .severity(severity)
                        .avgDurationSeconds(round1(avgDuration))
                        .occurrenceCount(count)
                        .failureRate(round1(failureRate))
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to build bottlenecks: {}", e.getMessage());
            return List.of();
        }
    }

    // -- Department Metrics --

    private List<DepartmentMetric> buildDepartmentMetrics(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("departmentId").exists(true).ne("")),
                    group("departmentId", "departmentName")
                            .count().as("totalRequests")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("status").is("APPROVED")).then(1).otherwise(0))
                            .as("completedRequests")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("status").in("PENDING_APPROVAL", "SUBMITTED"))
                                    .then(1).otherwise(0))
                            .as("pendingRequests"),
                    sort(Sort.Direction.DESC, "totalRequests"),
                    limit(20),
                    project("totalRequests", "completedRequests", "pendingRequests")
                            .and("_id.departmentId").as("departmentId")
                            .and("_id.departmentName").as("departmentName")
                            .and("completedRequests").divide("totalRequests").multiply(100).as("completionRate")
            );
            var results = mongoTemplate.aggregate(agg, "requests", Map.class);
            List<DepartmentMetric> metrics = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                String deptId = str(r.get("departmentId"));
                String deptName = str(r.get("departmentName"));
                long totalReqs = longVal(r.get("totalRequests"));
                long completedReqs = longVal(r.get("completedRequests"));
                long pendingReqs = longVal(r.get("pendingRequests"));
                double compRate = doubleVal(r.get("completionRate"));
                long deptUsers = mongoTemplate.count(
                        Query.query(Criteria.where("department").is(deptName)), "users");
                long activeDeptUsers = mongoTemplate.count(
                        Query.query(Criteria.where("department").is(deptName)
                                .and("lastLoginAt").gte(since)), "users");

                metrics.add(DepartmentMetric.builder()
                        .departmentId(deptId)
                        .departmentName(deptName)
                        .totalRequests(totalReqs)
                        .completedRequests(completedReqs)
                        .pendingRequests(pendingReqs)
                        .completionRate(round1(compRate))
                        .totalUsers(deptUsers)
                        .activeUsers(activeDeptUsers)
                        .build());
            }
            return metrics;
        } catch (Exception e) {
            log.warn("Failed to build department metrics: {}", e.getMessage());
            return List.of();
        }
    }

    // -- Computation helpers --

    private double computeAvgDurationMs(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("totalDurationMs").exists(true)
                            .and("completedAt").gte(since).lte(now)),
                    group().avg("totalDurationMs").as("avgDurationMs")
            );
            var results = mongoTemplate.aggregate(agg, "workflow_executions", Map.class);
            var r = results.getUniqueMappedResult();
            if (r != null && r.get("avgDurationMs") != null)
                return ((Number) r.get("avgDurationMs")).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to compute avg duration: {}", e.getMessage());
        }
        return 0;
    }

    private double computeAvgApprovalTimeHours(Instant since, Instant now) {
        try {
            var results = mongoTemplate.find(
                    Query.query(Criteria.where("completedAt").exists(true)
                            .and("submittedAt").exists(true)
                            .and("completedAt").gte(since).lte(now))
                            .with(Sort.by(Sort.Direction.DESC, "completedAt"))
                            .limit(5000),
                    Map.class, "requests");
            if (results.isEmpty()) return 0;
            return results.stream()
                    .filter(r -> r.get("completedAt") instanceof Instant && r.get("submittedAt") instanceof Instant)
                    .mapToLong(r -> ChronoUnit.MILLIS.between(
                            (Instant) r.get("submittedAt"), (Instant) r.get("completedAt")))
                    .average()
                    .orElse(0) / 3_600_000.0;
        } catch (Exception e) {
            log.warn("Failed to compute avg approval time: {}", e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private double computeSLACompliance(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("completedAt").exists(true)
                            .and("dueDate").exists(true)
                            .and("completedAt").gte(since).lte(now)),
                    project("completedAt", "dueDate")
                            .and(ComparisonOperators.Lte.valueOf("completedAt")
                                    .lessThanEqualTo("dueDate"))
                            .as("slaMet"),
                    group()
                            .count().as("total")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("slaMet").is(true))
                                    .then(1).otherwise(0))
                            .as("compliant"),
                    project("total", "compliant")
                            .and("compliant").divide("total").multiply(100).as("slaRate")
            );
            var results = mongoTemplate.aggregate(agg, "requests", Map.class);
            var r = results.getUniqueMappedResult();
            if (r != null && r.get("slaRate") != null)
                return ((Number) r.get("slaRate")).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to compute SLA: {}", e.getMessage());
        }
        return 100;
    }

    private double computeStepFailureRate(String stepId, Instant since) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("startedAt").gte(since).and("executionLog").exists(true)),
                    unwind("executionLog"),
                    match(Criteria.where("executionLog.stepId").is(stepId)),
                    group()
                            .count().as("total")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("executionLog.status").is("FAILED"))
                                    .then(1).otherwise(0))
                            .as("failures"),
                    project("total", "failures")
                            .and("failures").divide("total").multiply(100).as("rate")
            );
            var results = mongoTemplate.aggregate(agg, "workflow_executions", Map.class);
            var r = results.getUniqueMappedResult();
            if (r != null && r.get("rate") != null)
                return ((Number) r.get("rate")).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to compute step failure rate: {}", e.getMessage());
        }
        return 0;
    }

    private double computeStepAvgDurationMs(String stepId, Instant since) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("startedAt").gte(since).and("executionLog").exists(true)),
                    unwind("executionLog"),
                    match(Criteria.where("executionLog.stepId").is(stepId)
                            .and("executionLog.durationMs").exists(true)),
                    group().avg("executionLog.durationMs").as("avgMs")
            );
            var results = mongoTemplate.aggregate(agg, "workflow_executions", Map.class);
            var r = results.getUniqueMappedResult();
            if (r != null && r.get("avgMs") != null)
                return ((Number) r.get("avgMs")).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to compute step avg duration: {}", e.getMessage());
        }
        return 0;
    }

    // -- Trend computation --

    @SuppressWarnings("unchecked")
    private List<TimeSeriesPoint> computeDailyTrend(String collection, String dateField,
                                                      Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where(dateField).gte(since).lte(now)),
                    project().and(dateField).as("dt"),
                    project().and(DateOperators.dateOf("dt").toString("%Y-%m-%d")).as("period"),
                    group("period").count().as("count"),
                    sort(Sort.Direction.ASC, "_id"),
                    project("count").and("_id").as("period").andExclude("_id")
            );
            var results = mongoTemplate.aggregate(agg, collection, Map.class);
            List<TimeSeriesPoint> points = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                points.add(TimeSeriesPoint.builder()
                        .period(str(r.get("period")))
                        .count(longVal(r.get("count")))
                        .build());
            }
            return points;
        } catch (Exception e) {
            log.warn("Failed to compute daily trend for {}/{}: {}", collection, dateField, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<TimeSeriesPoint> computeApprovalTrend(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("completedAt").gte(since).lte(now)
                            .and("status").in("APPROVED", "REJECTED")),
                    project().and("completedAt").as("dt").and("status").as("status"),
                    project()
                            .and(DateOperators.dateOf("dt").toString("%Y-%m-%d")).as("period")
                            .and("status").as("status"),
                    group("period")
                            .count().as("count")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("status").is("APPROVED")).then(1).otherwise(0))
                            .as("successCount")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("status").is("REJECTED")).then(1).otherwise(0))
                            .as("failureCount"),
                    sort(Sort.Direction.ASC, "_id"),
                    project("count", "successCount", "failureCount")
                            .and("_id").as("period").andExclude("_id")
            );
            var results = mongoTemplate.aggregate(agg, "requests", Map.class);
            List<TimeSeriesPoint> points = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                points.add(TimeSeriesPoint.builder()
                        .period(str(r.get("period")))
                        .count(longVal(r.get("count")))
                        .successCount(longVal(r.get("successCount")))
                        .failureCount(longVal(r.get("failureCount")))
                        .build());
            }
            return points;
        } catch (Exception e) {
            log.warn("Failed to compute approval trend: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<TimeSeriesPoint> computeLoginTrend(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("timestamp").gte(since).lte(now)
                            .and("action").is("LOGIN")),
                    project().and("timestamp").as("dt"),
                    project().and(DateOperators.dateOf("dt").toString("%Y-%m-%d")).as("period"),
                    group("period").count().as("count"),
                    sort(Sort.Direction.ASC, "_id"),
                    project("count").and("_id").as("period").andExclude("_id")
            );
            var results = mongoTemplate.aggregate(agg, "login_audits", Map.class);
            List<TimeSeriesPoint> points = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                points.add(TimeSeriesPoint.builder()
                        .period(str(r.get("period")))
                        .count(longVal(r.get("count")))
                        .build());
            }
            return points;
        } catch (Exception e) {
            log.warn("Failed to compute login trend: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<TimeSeriesPoint> computeFraudTrend(Instant since, Instant now) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("checkedAt").gte(since).lte(now)),
                    project().and("checkedAt").as("dt").and("riskLevel").as("riskLevel"),
                    project()
                            .and(DateOperators.dateOf("dt").toString("%Y-%m-%d")).as("period")
                            .and("riskLevel").as("riskLevel"),
                    group("period")
                            .count().as("count")
                            .sum(ConditionalOperators.when(
                                    Criteria.where("riskLevel").in("HIGH", "CRITICAL"))
                                    .then(1).otherwise(0))
                            .as("failureCount"),
                    sort(Sort.Direction.ASC, "_id"),
                    project("count", "failureCount")
                            .and("_id").as("period").andExclude("_id")
            );
            var results = mongoTemplate.aggregate(agg, "fraud_checks", Map.class);
            List<TimeSeriesPoint> points = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                points.add(TimeSeriesPoint.builder()
                        .period(str(r.get("period")))
                        .count(longVal(r.get("count")))
                        .failureCount(longVal(r.get("failureCount")))
                        .build());
            }
            return points;
        } catch (Exception e) {
            log.warn("Failed to compute fraud trend: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<UserActivityItem> getTopActiveUsers(Instant since) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where("timestamp").gte(since)),
                    group("performedBy")
                            .count().as("actionCount")
                            .addToSet("action").as("actions"),
                    sort(Sort.Direction.DESC, "actionCount"),
                    limit(10),
                    project("actionCount", "actions").and("_id").as("userId")
            );
            var results = mongoTemplate.aggregate(agg, "audit_logs", Map.class);
            List<UserActivityItem> items = new ArrayList<>();
            for (var r : results.getMappedResults()) {
                String userId = str(r.get("userId"));
                long count = longVal(r.get("actionCount"));
                @SuppressWarnings("unchecked")
                List<String> actions = (List<String>) r.get("actions");
                String topAction = (actions != null && !actions.isEmpty()) ? actions.get(0) : null;
                items.add(UserActivityItem.builder()
                        .userId(userId)
                        .actionCount(count)
                        .topAction(topAction)
                        .build());
            }
            return items;
        } catch (Exception e) {
            log.warn("Failed to get top active users: {}", e.getMessage());
            return List.of();
        }
    }

    // -- Generic aggregation helpers --

    @SuppressWarnings("unchecked")
    private Map<String, Long> groupByStringField(String collection, String field,
                                                   Instant since, Instant now) {
        try {
            List<AggregationOperation> ops = new ArrayList<>();
            if (since != null && now != null) {
                ops.add(match(Criteria.where("createdAt").gte(since).lte(now)));
            }
            ops.add(group(field).count().as("count"));
            ops.add(sort(Sort.Direction.DESC, "count"));
            ops.add(project("count").and("_id").as("name"));
            var results = mongoTemplate.aggregate(
                    newAggregation(ops), collection, Map.class);
            Map<String, Long> result = new LinkedHashMap<>();
            for (var r : results.getMappedResults()) {
                Object name = r.get("name");
                if (name != null) result.put(name.toString(), longVal(r.get("count")));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to groupByStringField {}/{}: {}", collection, field, e.getMessage());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> groupAllStringField(String collection, String field) {
        try {
            Aggregation agg = newAggregation(
                    match(Criteria.where(field).exists(true).ne("").ne(null)),
                    group(field).count().as("count"),
                    sort(Sort.Direction.DESC, "count"),
                    project("count").and("_id").as("name")
            );
            var results = mongoTemplate.aggregate(agg, collection, Map.class);
            Map<String, Long> result = new LinkedHashMap<>();
            for (var r : results.getMappedResults()) {
                Object name = r.get("name");
                if (name != null) result.put(name.toString(), longVal(r.get("count")));
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to groupAllStringField {}/{}: {}", collection, field, e.getMessage());
            return Map.of();
        }
    }

    private long countByField(String collection, String field, String value) {
        try {
            return mongoTemplate.count(Query.query(Criteria.where(field).is(value)), collection);
        } catch (Exception e) {
            log.warn("Failed to countByField {}/{}/{}: {}", collection, field, value, e.getMessage());
            return 0;
        }
    }

    private long aggregateSum(String collection, String field) {
        try {
            var results = mongoTemplate.aggregate(
                    newAggregation(group().sum(field).as("total")),
                    collection, Map.class);
            var r = results.getUniqueMappedResult();
            return r != null ? longVal(r.get("total")) : 0;
        } catch (Exception e) {
            log.warn("Failed to aggregateSum {}/{}: {}", collection, field, e.getMessage());
            return 0;
        }
    }

    private double aggregateAvg(String collection, String field) {
        try {
            var results = mongoTemplate.aggregate(
                    newAggregation(match(Criteria.where(field).exists(true)),
                            group().avg(field).as("avg")),
                    collection, Map.class);
            var r = results.getUniqueMappedResult();
            if (r != null && r.get("avg") != null)
                return ((Number) r.get("avg")).doubleValue();
        } catch (Exception e) {
            log.warn("Failed to aggregateAvg {}/{}: {}", collection, field, e.getMessage());
        }
        return 0;
    }

    // -- Type converters --

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private static long longVal(Object o) {
        if (o == null) return 0;
        return ((Number) o).longValue();
    }

    private static double doubleVal(Object o) {
        if (o == null) return 0;
        return ((Number) o).doubleValue();
    }

    // -- Cache eviction --

    @Scheduled(fixedRateString = "${app.analytics.cache-evict-interval:300000}")
    @CacheEvict(value = {"analytics-summary", "analytics-workflows", "analytics-workflow-metrics",
            "analytics-request-metrics", "analytics-user-metrics", "analytics-document-metrics",
            "analytics-fraud-metrics", "analytics-notification-metrics", "analytics-ai-metrics",
            "analytics-scheduler-metrics", "analytics-bottlenecks", "analytics-department-metrics"},
            allEntries = true)
    public void evictAllCache() {
        log.info("Evicting all analytics caches");
    }
}
