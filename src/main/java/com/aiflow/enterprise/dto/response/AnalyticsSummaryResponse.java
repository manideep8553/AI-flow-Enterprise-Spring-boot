package com.aiflow.enterprise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsSummaryResponse {

    private KPIMetrics kpi;
    private WorkflowMetrics workflows;
    private RequestMetrics requests;
    private UserMetrics users;
    private DocumentMetrics documents;
    private FraudMetrics fraud;
    private NotificationMetrics notifications;
    private AIMetrics ai;
    private SchedulerMetrics scheduler;
    private List<BottleneckItem> bottlenecks;
    private List<DepartmentMetric> departments;
    private Instant generatedAt;
    private String period;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class KPIMetrics {
        private long totalExecutions;
        private long completedExecutions;
        private long failedExecutions;
        private double completionRate;
        private double avgDurationSeconds;
        private double avgDurationChange;
        private long activeUsers;
        private long totalUsers;
        private double activeUserChange;
        private long totalRequests;
        private long pendingApprovals;
        private double requestChange;
        private long totalDocuments;
        private long totalStorageBytes;
        private long openFraudAlerts;
        private long unreadNotifications;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class WorkflowMetrics {
        private long total;
        private long running;
        private long completed;
        private long failed;
        private long cancelled;
        private long suspended;
        private double successRate;
        private double avgDurationSeconds;
        private long totalThisPeriod;
        private List<TimeSeriesPoint> trend;
        private Map<String, Long> byWorkflow;
        private Map<String, Long> byTriggerType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RequestMetrics {
        private long total;
        private long submitted;
        private long pendingApproval;
        private long approved;
        private long rejected;
        private long cancelled;
        private double approvalRate;
        private double avgApprovalTimeHours;
        private double slaCompliance;
        private long escalated;
        private Map<String, Long> byType;
        private Map<String, Long> byPriority;
        private List<TimeSeriesPoint> trend;
        private List<TimeSeriesPoint> approvalTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserMetrics {
        private long total;
        private long active;
        private long newThisPeriod;
        private long withSessions;
        private Map<String, Long> byRole;
        private List<TimeSeriesPoint> loginTrend;
        private List<UserActivityItem> topActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentMetrics {
        private long total;
        private long processing;
        private long completed;
        private long failed;
        private long archived;
        private long totalStorageBytes;
        private double avgFileSizeBytes;
        private Map<String, Long> byType;
        private Map<String, Long> byCategory;
        private List<TimeSeriesPoint> uploadTrend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FraudMetrics {
        private long totalChecks;
        private long confirmed;
        private long dismissed;
        private long pendingReview;
        private long escalated;
        private long openAlerts;
        private double avgRiskScore;
        private Map<String, Long> byRiskLevel;
        private Map<String, Long> byDepartment;
        private List<TimeSeriesPoint> trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class NotificationMetrics {
        private long total;
        private long sent;
        private long delivered;
        private long failed;
        private long pending;
        private long unread;
        private double deliveryRate;
        private Map<String, Long> byType;
        private Map<String, Long> byChannel;
        private List<TimeSeriesPoint> trend;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AIMetrics {
        private long totalCalls;
        private long accepted;
        private long feedbackProvided;
        private double acceptanceRate;
        private double avgResponseTimeMs;
        private long totalTokensUsed;
        private Map<String, Long> byRequestType;
        private Map<String, Long> byConfidenceLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SchedulerMetrics {
        private long totalJobs;
        private long running;
        private long completedToday;
        private long failedToday;
        private double successRate;
        private long activeLocks;
        private List<JobHealthItem> jobs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BottleneckItem {
        private String workflowId;
        private String workflowName;
        private String stepId;
        private String stepName;
        private String severity;
        private double avgDurationSeconds;
        private long occurrenceCount;
        private double failureRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DepartmentMetric {
        private String departmentId;
        private String departmentName;
        private long totalRequests;
        private long completedRequests;
        private long pendingRequests;
        private double completionRate;
        private double avgProcessingTimeHours;
        private long totalUsers;
        private long activeUsers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeSeriesPoint {
        private String period;
        private long count;
        private long successCount;
        private long failureCount;
        private Double value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserActivityItem {
        private String userId;
        private String username;
        private String email;
        private long actionCount;
        private String topAction;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class JobHealthItem {
        private String jobName;
        private String jobGroup;
        private String status;
        private Instant lastRun;
        private long totalRuns;
        private long failures;
        private double successRate;
        private Double avgDurationMs;
    }
}
