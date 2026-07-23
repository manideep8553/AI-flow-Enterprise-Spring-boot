package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.response.AnalyticsSummaryResponse;

public interface AnalyticsService {

    AnalyticsSummaryResponse getSummary(String period, String department, String workflowId);

    AnalyticsSummaryResponse getWorkflowAnalytics(String period);

    AnalyticsSummaryResponse.WorkflowMetrics getWorkflowMetrics(String period);

    AnalyticsSummaryResponse.RequestMetrics getRequestMetrics(String period);

    AnalyticsSummaryResponse.UserMetrics getUserMetrics(String period);

    AnalyticsSummaryResponse.DocumentMetrics getDocumentMetrics();

    AnalyticsSummaryResponse.FraudMetrics getFraudMetrics(String period);

    AnalyticsSummaryResponse.NotificationMetrics getNotificationMetrics(String period);

    AnalyticsSummaryResponse.AIMetrics getAIMetrics(String period);

    AnalyticsSummaryResponse.SchedulerMetrics getSchedulerMetrics();

    java.util.List<AnalyticsSummaryResponse.BottleneckItem> getBottlenecks();

    java.util.List<AnalyticsSummaryResponse.DepartmentMetric> getDepartmentMetrics(String period);

    void evictAllCache();
}
