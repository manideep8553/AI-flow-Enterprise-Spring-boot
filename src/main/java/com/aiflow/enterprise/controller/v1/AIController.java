package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.ai.AIDecisionService;
import com.aiflow.enterprise.dto.request.AIFeedbackRequest;
import com.aiflow.enterprise.dto.request.AIQueryRequest;
import com.aiflow.enterprise.dto.response.AIInsightResponse;
import com.aiflow.enterprise.dto.response.AIResponse;
import com.aiflow.enterprise.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI Decision Engine", description = "AI-powered decision engine for workflow approvals and insights")
public class AIController {

    private final AIDecisionService aiDecisionService;

    public AIController(AIDecisionService aiDecisionService) {
        this.aiDecisionService = aiDecisionService;
    }

    @PostMapping("/query")
    @Operation(summary = "Send a conversational AI query about workflows and requests")
    public ResponseEntity<ApiResponse<AIResponse>> conversationalQuery(
            @Valid @RequestBody AIQueryRequest queryRequest) {
        AIResponse response = aiDecisionService.conversationalQuery(queryRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recommendations/approval/{requestId}")
    @Operation(summary = "Get AI-powered approval recommendation for a request")
    public ResponseEntity<ApiResponse<AIResponse>> getApprovalRecommendation(
            @PathVariable String requestId) {
        AIResponse response = aiDecisionService.getApprovalRecommendation(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summaries/request/{requestId}")
    @Operation(summary = "Get AI-generated summary of a request")
    public ResponseEntity<ApiResponse<AIResponse>> getRequestSummary(
            @PathVariable String requestId) {
        AIResponse response = aiDecisionService.getRequestSummary(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/explain/{executionId}")
    @Operation(summary = "Explain a decision made during workflow execution")
    public ResponseEntity<ApiResponse<AIResponse>> explainDecision(
            @PathVariable String executionId,
            @RequestParam(required = false) String stepId) {
        AIResponse response = aiDecisionService.explainDecision(executionId, stepId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/predict/{requestId}")
    @Operation(summary = "Predict the approval outcome of a request")
    public ResponseEntity<ApiResponse<AIResponse>> predictOutcome(
            @PathVariable String requestId) {
        AIResponse response = aiDecisionService.predictOutcome(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recommendations/approver/{requestId}")
    @Operation(summary = "Recommend the best approver for a request")
    public ResponseEntity<ApiResponse<AIResponse>> recommendApprover(
            @PathVariable String requestId) {
        AIResponse response = aiDecisionService.recommendApprover(requestId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/optimize/{workflowId}")
    @Operation(summary = "Get workflow optimization suggestions")
    public ResponseEntity<ApiResponse<List<AIInsightResponse>>> optimizeWorkflow(
            @PathVariable String workflowId) {
        List<AIInsightResponse> insights = aiDecisionService.optimizeWorkflow(workflowId);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    @GetMapping("/bottlenecks")
    @Operation(summary = "Detect performance bottlenecks across workflows")
    public ResponseEntity<ApiResponse<List<AIInsightResponse>>> detectBottlenecks() {
        List<AIInsightResponse> insights = aiDecisionService.detectBottlenecks();
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    @GetMapping("/insights/{workflowId}")
    @Operation(summary = "Generate actionable insights from workflow data")
    public ResponseEntity<ApiResponse<List<AIInsightResponse>>> generateInsights(
            @PathVariable String workflowId) {
        List<AIInsightResponse> insights = aiDecisionService.generateInsights(workflowId);
        return ResponseEntity.ok(ApiResponse.success(insights));
    }

    @GetMapping("/trends")
    @Operation(summary = "Analyze workflow execution trends")
    public ResponseEntity<ApiResponse<AIResponse>> analyzeTrends(
            @RequestParam(defaultValue = "30d") String timeframe,
            @RequestParam(defaultValue = "completion_rate") String metric) {
        AIResponse response = aiDecisionService.analyzeTrends(timeframe, metric);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare multiple workflows")
    public ResponseEntity<ApiResponse<AIResponse>> compareWorkflows(
            @RequestBody List<String> workflowIds) {
        AIResponse response = aiDecisionService.compareWorkflows(workflowIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/report/{workflowId}")
    @Operation(summary = "Generate an AI-powered report for a workflow")
    public ResponseEntity<ApiResponse<AIResponse>> generateReport(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "performance") String reportType) {
        AIResponse response = aiDecisionService.generateReport(workflowId, reportType);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/feedback")
    @Operation(summary = "Provide feedback on an AI decision")
    public ResponseEntity<ApiResponse<Void>> provideFeedback(
            @Valid @RequestBody AIFeedbackRequest feedbackRequest) {
        aiDecisionService.provideFeedback(
                feedbackRequest.getLogId(),
                feedbackRequest.isAccepted(),
                feedbackRequest.isPositive(),
                feedbackRequest.getComment());
        return ResponseEntity.ok(ApiResponse.success(null, "Feedback recorded"));
    }
}
