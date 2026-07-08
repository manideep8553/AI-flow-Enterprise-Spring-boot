package com.aiflow.enterprise.ai;

import com.aiflow.enterprise.dto.request.AIQueryRequest;
import com.aiflow.enterprise.dto.response.AIInsightResponse;
import com.aiflow.enterprise.dto.response.AIResponse;

import java.util.List;
import java.util.Map;

public interface AIDecisionService {

    AIResponse getApprovalRecommendation(String requestId);

    AIResponse getRequestSummary(String requestId);

    AIResponse explainDecision(String executionId, String stepId);

    AIResponse predictOutcome(String requestId);

    AIResponse recommendApprover(String requestId);

    List<AIInsightResponse> optimizeWorkflow(String workflowId);

    List<AIInsightResponse> detectBottlenecks();

    List<AIInsightResponse> generateInsights(String workflowId);

    AIResponse conversationalQuery(AIQueryRequest queryRequest);

    AIResponse analyzeTrends(String timeframe, String metric);

    AIResponse compareWorkflows(List<String> workflowIds);

    AIResponse generateReport(String workflowId, String reportType);

    void provideFeedback(String logId, boolean accepted, boolean positive, String comment);
}
