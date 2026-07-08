package com.aiflow.enterprise.ai;

import com.aiflow.enterprise.dto.request.AIQueryRequest;
import com.aiflow.enterprise.dto.response.AIInsightResponse;
import com.aiflow.enterprise.dto.response.AIResponse;
import com.aiflow.enterprise.entity.AIDecisionLog;
import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.repository.AIDecisionLogRepository;
import com.aiflow.enterprise.repository.RequestRepository;
import com.aiflow.enterprise.repository.WorkflowExecutionRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AIDecisionServiceImpl implements AIDecisionService {

    private static final Logger log = LoggerFactory.getLogger(AIDecisionServiceImpl.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final AIDecisionLogRepository logRepository;
    private final RequestRepository requestRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;

    @Value("${ai.decision.min-confidence:0.5}")
    private double minConfidence;

    public AIDecisionServiceImpl(ChatClient chatClient,
                                 ObjectMapper objectMapper,
                                 AIDecisionLogRepository logRepository,
                                 RequestRepository requestRepository,
                                 WorkflowExecutionRepository executionRepository,
                                 WorkflowRepository workflowRepository) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.logRepository = logRepository;
        this.requestRepository = requestRepository;
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse getApprovalRecommendation(String requestId) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return errorResponse("Request not found", "APPROVAL_RECOMMENDATION");
        }

        String prompt = buildApprovalPrompt(request);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.APPROVAL_RECOMMENDATION, request);
        aiResp.setType("APPROVAL_RECOMMENDATION");
        aiResp.setId(requestId);
        logDecision(AIRequestType.APPROVAL_RECOMMENDATION, prompt, aiResp, request, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse getRequestSummary(String requestId) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return errorResponse("Request not found", "REQUEST_SUMMARY");
        }

        String prompt = buildSummaryPrompt(request);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.REQUEST_SUMMARY, request);
        aiResp.setType("REQUEST_SUMMARY");
        aiResp.setId(requestId);
        logDecision(AIRequestType.REQUEST_SUMMARY, prompt, aiResp, request, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse explainDecision(String executionId, String stepId) {
        WorkflowExecution execution = executionRepository.findById(executionId).orElse(null);
        if (execution == null) {
            return errorResponse("Execution not found", "DECISION_EXPLANATION");
        }

        Workflow workflow = workflowRepository.findById(execution.getWorkflowId()).orElse(null);
        String prompt = buildExplanationPrompt(execution, workflow, stepId);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.DECISION_EXPLANATION, null);
        aiResp.setType("DECISION_EXPLANATION");
        aiResp.setId(executionId);
        logDecision(AIRequestType.DECISION_EXPLANATION, prompt, aiResp, null, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse predictOutcome(String requestId) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return errorResponse("Request not found", "OUTCOME_PREDICTION");
        }

        var historyPage = requestRepository.findByRequestTypeId(request.getRequestTypeId(),
                org.springframework.data.domain.PageRequest.of(0, 50));
        List<Request> history = historyPage.getContent();
        String prompt = buildPredictionPrompt(request, history);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.OUTCOME_PREDICTION, request);
        aiResp.setType("OUTCOME_PREDICTION");
        aiResp.setId(requestId);
        logDecision(AIRequestType.OUTCOME_PREDICTION, prompt, aiResp, request, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse recommendApprover(String requestId) {
        Request request = requestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return errorResponse("Request not found", "APPROVER_RECOMMENDATION");
        }

        String prompt = buildApproverPrompt(request);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.APPROVER_RECOMMENDATION, request);
        aiResp.setType("APPROVER_RECOMMENDATION");
        aiResp.setId(requestId);
        logDecision(AIRequestType.APPROVER_RECOMMENDATION, prompt, aiResp, request, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIInsightResponse> optimizeWorkflow(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) return List.of(errorInsight("Workflow not found"));

        List<WorkflowExecution> executions = executionRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        String prompt = buildOptimizationPrompt(workflow, executions);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.WORKFLOW_OPTIMIZATION, null);
        logDecision(AIRequestType.WORKFLOW_OPTIMIZATION, prompt, aiResp, null, start);
        return parseInsights(aiResp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIInsightResponse> detectBottlenecks() {
        List<WorkflowExecution> recentExecutions = executionRepository
                .findByStatusIn(List.of(com.aiflow.enterprise.enums.ExecutionStatus.RUNNING,
                        com.aiflow.enterprise.enums.ExecutionStatus.PENDING,
                        com.aiflow.enterprise.enums.ExecutionStatus.SUSPENDED));
        String prompt = buildBottleneckPrompt(recentExecutions);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.BOTTLENECK_DETECTION, null);
        logDecision(AIRequestType.BOTTLENECK_DETECTION, prompt, aiResp, null, start);
        return parseInsights(aiResp);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AIInsightResponse> generateInsights(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) return List.of(errorInsight("Workflow not found"));

        List<WorkflowExecution> executions = executionRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        String prompt = buildInsightPrompt(workflow, executions);
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.INSIGHT_GENERATION, null);
        logDecision(AIRequestType.INSIGHT_GENERATION, prompt, aiResp, null, start);
        return parseInsights(aiResp);
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse conversationalQuery(AIQueryRequest queryRequest) {
        String context = buildConversationalContext(queryRequest);
        String prompt = "Context:\n" + context + "\n\nUser Query: " + queryRequest.getQuery()
                + "\n\nProvide a helpful, concise response based on the context above.";
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.CONVERSATIONAL_QUERY, null);
        aiResp.setType("CONVERSATIONAL_QUERY");
        aiResp.setId(queryRequest.getContextId());
        logDecision(AIRequestType.CONVERSATIONAL_QUERY, prompt, aiResp, null, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse analyzeTrends(String timeframe, String metric) {
        String prompt = "Analyze workflow execution trends for the timeframe: " + timeframe
                + "\nMetric: " + metric
                + "\nProvide trend analysis with confidence scores and reasoning.";
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.INSIGHT_GENERATION, null);
        aiResp.setType("TREND_ANALYSIS");
        logDecision(AIRequestType.INSIGHT_GENERATION, prompt, aiResp, null, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse compareWorkflows(List<String> workflowIds) {
        List<Workflow> workflows = new ArrayList<>();
        for (String id : workflowIds) {
            workflowRepository.findById(id).ifPresent(workflows::add);
        }
        StringBuilder prompt = new StringBuilder("Compare the following workflows:\n\n");
        for (Workflow wf : workflows) {
            prompt.append("- ").append(wf.getName())
                    .append(" (v").append(wf.getVersion()).append("): ")
                    .append(wf.getDescription() != null ? wf.getDescription() : "No description")
                    .append("\n");
        }
        prompt.append("\nProvide comparison with strengths, weaknesses, and recommendations.");
        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt.toString(), AIRequestType.INSIGHT_GENERATION, null);
        aiResp.setType("WORKFLOW_COMPARISON");
        logDecision(AIRequestType.INSIGHT_GENERATION, prompt.toString(), aiResp, null, start);
        return aiResp;
    }

    @Override
    @Transactional(readOnly = true)
    public AIResponse generateReport(String workflowId, String reportType) {
        Workflow workflow = workflowRepository.findById(workflowId).orElse(null);
        if (workflow == null) return errorResponse("Workflow not found", "REPORT");

        List<WorkflowExecution> executions = executionRepository
                .findByWorkflowIdOrderByCreatedAtDesc(workflowId);
        String prompt = "Generate a " + reportType + " report for workflow: " + workflow.getName()
                + "\n\nWorkflow Details:\n" + workflowToText(workflow)
                + "\n\nExecution History (" + executions.size() + " executions):\n";
        for (WorkflowExecution exec : executions) {
            prompt += "- " + exec.getStatus() + " | started: " + exec.getStartedAt()
                    + " | duration: " + exec.getTotalDurationMs() + "ms\n";
        }
        prompt += "\nProvide a comprehensive " + reportType + " report with data-driven insights.";

        long start = System.currentTimeMillis();
        AIResponse aiResp = callAI(prompt, AIRequestType.INSIGHT_GENERATION, null);
        aiResp.setType("REPORT_" + reportType.toUpperCase());
        aiResp.setId(workflowId);
        logDecision(AIRequestType.INSIGHT_GENERATION, prompt, aiResp, null, start);
        return aiResp;
    }

    @Override
    public void provideFeedback(String logId, boolean accepted, boolean positive, String comment) {
        logRepository.findById(logId).ifPresent(entry -> {
            entry.setFeedbackProvided(true);
            entry.setFeedbackPositive(positive);
            entry.setAccepted(accepted);
            logRepository.save(entry);
            log.info("AI feedback recorded for log {}: accepted={}, positive={}", logId, accepted, positive);
        });
    }

    private AIResponse callAI(String prompt, AIRequestType requestType, Request request) {
        try {
            String systemPrompt = getSystemPrompt(requestType);

            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(prompt)
                    .call()
                    .content();

            if (content == null) content = "";

            Map<String, Object> parsed = parseAIResponse(content);

            @SuppressWarnings("unchecked")
            Map<String, Object> rawMetrics = (Map<String, Object>) parsed.getOrDefault("metrics", Map.of());
            Map<String, Double> metrics = new HashMap<>();
            if (rawMetrics != null) {
                for (Map.Entry<String, Object> e : rawMetrics.entrySet()) {
                    if (e.getValue() instanceof Number) {
                        metrics.put(e.getKey(), ((Number) e.getValue()).doubleValue());
                    }
                }
            }

            AIResponse aiResp = AIResponse.builder()
                    .summary(getString(parsed, "summary", ""))
                    .detailedAnalysis(getString(parsed, "detailedAnalysis",
                            getString(parsed, "analysis", content)))
                    .recommendation(getString(parsed, "recommendation", ""))
                    .confidenceLevel(parseConfidence(getDouble(parsed, "confidenceScore", 0.7)))
                    .confidenceScore(getDouble(parsed, "confidenceScore", 0.7))
                    .reasoning(getString(parsed, "reasoning", ""))
                    .alternatives(getList(parsed, "alternatives"))
                    .data(getMap(parsed, "data"))
                    .metrics(metrics)
                    .warnings(getList(parsed, "warnings"))
                    .modelUsed("gpt-4o-mini")
                    .build();

            return aiResp;

        } catch (Exception e) {
            log.error("AI call failed for type {}: {}", requestType, e.getMessage());
            return AIResponse.builder()
                    .summary("AI analysis unavailable")
                    .detailedAnalysis("Error: " + e.getMessage())
                    .recommendation("Unable to generate recommendation at this time")
                    .confidenceLevel(ConfidenceLevel.VERY_LOW)
                    .confidenceScore(0.0)
                    .reasoning("Service error: " + e.getMessage())
                    .modelUsed("fallback")
                    .build();
        }
    }

    private AIResponse errorResponse(String message, String type) {
        return AIResponse.builder()
                .id("error")
                .type(type)
                .summary(message)
                .detailedAnalysis(message)
                .recommendation("Unable to process request")
                .confidenceLevel(ConfidenceLevel.VERY_LOW)
                .confidenceScore(0.0)
                .reasoning(message)
                .build();
    }

    private AIInsightResponse errorInsight(String message) {
        return AIInsightResponse.builder()
                .title("Error")
                .description(message)
                .confidenceLevel(ConfidenceLevel.VERY_LOW)
                .confidenceScore(0.0)
                .reasoning(message)
                .severity("HIGH")
                .build();
    }

    private void logDecision(AIRequestType type, String prompt, AIResponse response,
                             Request request, long startTime) {
        try {
            long duration = System.currentTimeMillis() - startTime;
            AIDecisionLog logEntry = AIDecisionLog.builder()
                    .requestType(type)
                    .modelUsed(response.getModelUsed())
                    .promptTokens(response.getPromptTokens())
                    .completionTokens(response.getCompletionTokens())
                    .totalTokens(response.getPromptTokens() + response.getCompletionTokens())
                    .responseTimeMs(duration)
                    .requestId(request != null ? request.getId() : null)
                    .requestTitle(request != null ? request.getTitle() : null)
                    .workflowId(request != null ? request.getWorkflowId() : null)
                    .aiResponse(response.getDetailedAnalysis())
                    .recommendation(response.getRecommendation())
                    .confidenceLevel(response.getConfidenceLevel())
                    .confidenceScore(response.getConfidenceScore())
                    .reasoning(response.getReasoning())
                    .accepted(false)
                    .createdAt(Instant.now())
                    .build();
            logRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to log AI decision: {}", e.getMessage());
        }
    }

    private String getSystemPrompt(AIRequestType type) {
        return switch (type) {
            case APPROVAL_RECOMMENDATION ->
                    "You are an AI approval assistant. Analyze the request details and provide an approval recommendation. "
                    + "Return JSON with: summary, detailedAnalysis, recommendation (APPROVE/REJECT/REQUEST_INFO), "
                    + "confidenceScore (0.0-1.0), reasoning, alternatives (list), warnings (list). "
                    + "Be conservative - only recommend APPROVE with high confidence when evidence strongly supports it.";
            case REQUEST_SUMMARY ->
                    "You are an AI request summarizer. Create a concise, informative summary of the request. "
                    + "Return JSON with: summary, detailedAnalysis, confidenceScore, reasoning.";
            case DECISION_EXPLANATION ->
                    "You are an AI decision explainer. Explain why specific decisions were made in the workflow. "
                    + "Return JSON with: summary, detailedAnalysis, confidenceScore, reasoning, data (map with supporting evidence).";
            case OUTCOME_PREDICTION ->
                    "You are an AI outcome predictor. Analyze historical patterns to predict request outcomes. "
                    + "Return JSON with: summary, detailedAnalysis, recommendation (PREDICTED_APPROVE/PREDICTED_REJECT/UNCERTAIN), "
                    + "confidenceScore, reasoning, metrics (map with prediction factors).";
            case APPROVER_RECOMMENDATION ->
                    "You are an AI approver recommender. Based on request details and history, recommend the best approver. "
                    + "Return JSON with: summary, recommendation (approver identifier or role), "
                    + "confidenceScore, reasoning, alternatives (list of alternative approvers).";
            case WORKFLOW_OPTIMIZATION ->
                    "You are an AI workflow optimization expert. Analyze workflow structure and execution data. "
                    + "Return a JSON object with an 'insights' array. Each insight has: type, title, description, "
                    + "confidenceScore, reasoning, actionItems (list), metrics (map), severity (LOW/MEDIUM/HIGH/CRITICAL), category.";
            case BOTTLENECK_DETECTION ->
                    "You are an AI bottleneck detection specialist. Identify performance bottlenecks from execution data. "
                    + "Return a JSON object with an 'insights' array. Each insight has: type, title, description, "
                    + "confidenceScore, reasoning, actionItems (list), metrics (map), severity, category.";
            case INSIGHT_GENERATION ->
                    "You are an AI workflow insights analyst. Generate actionable insights from workflow data. "
                    + "Return a JSON object with an 'insights' array. Each insight has: type, title, description, "
                    + "confidenceScore, reasoning, actionItems (list), metrics (map), severity, category.";
            case CONVERSATIONAL_QUERY ->
                    "You are an AI workflow assistant answering user questions about workflows, requests, and approvals. "
                    + "Return JSON with: summary, detailedAnalysis, confidenceScore, reasoning, data (relevant context).";
        };
    }

    private Map<String, Object> parseAIResponse(String content) {
        try {
            int jsonStart = content.indexOf('{');
            int jsonEnd = content.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                String json = content.substring(jsonStart, jsonEnd + 1);
                return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON: {}", e.getMessage());
        }
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("summary", content.length() > 200 ? content.substring(0, 200) + "..." : content);
        fallback.put("detailedAnalysis", content);
        return fallback;
    }

    private ConfidenceLevel parseConfidence(double score) {
        if (score >= 0.9) return ConfidenceLevel.VERY_HIGH;
        if (score >= 0.7) return ConfidenceLevel.HIGH;
        if (score >= 0.5) return ConfidenceLevel.MEDIUM;
        if (score >= 0.3) return ConfidenceLevel.LOW;
        return ConfidenceLevel.VERY_LOW;
    }

    private List<AIInsightResponse> parseInsights(AIResponse response) {
        List<AIInsightResponse> insights = new ArrayList<>();
        try {
            if (response.getData() != null && response.getData().containsKey("insights")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> raw = (List<Map<String, Object>>) response.getData().get("insights");
                if (raw != null) {
                    for (Map<String, Object> item : raw) {
                        insights.add(mapToInsight(item));
                    }
                    return insights;
                }
            }
            AIInsightResponse fallback = AIInsightResponse.builder()
                    .title(response.getSummary())
                    .description(response.getDetailedAnalysis())
                    .confidenceLevel(response.getConfidenceLevel())
                    .confidenceScore(response.getConfidenceScore())
                    .reasoning(response.getReasoning())
                    .severity("MEDIUM")
                    .build();
            insights.add(fallback);
        } catch (Exception e) {
            log.warn("Failed to parse insights: {}", e.getMessage());
        }
        return insights;
    }

    @SuppressWarnings("unchecked")
    private AIInsightResponse mapToInsight(Map<String, Object> map) {
        return AIInsightResponse.builder()
                .type(getString(map, "type", "INSIGHT"))
                .title(getString(map, "title", ""))
                .description(getString(map, "description", ""))
                .confidenceLevel(parseConfidence(getDouble(map, "confidenceScore", 0.5)))
                .confidenceScore(getDouble(map, "confidenceScore", 0.5))
                .reasoning(getString(map, "reasoning", ""))
                .actionItems(getList(map, "actionItems"))
                .metrics(getMap(map, "metrics"))
                .severity(getString(map, "severity", "MEDIUM"))
                .category(getString(map, "category", "GENERAL"))
                .build();
    }

    private String buildApprovalPrompt(Request request) {
        return "Analyze this request for approval recommendation:\n\n"
                + "Title: " + request.getTitle() + "\n"
                + "Type: " + request.getRequestTypeName() + "\n"
                + "Status: " + request.getStatus() + "\n"
                + "Submitted by: " + request.getSubmittedByName() + "\n"
                + "Priority: " + request.getPriority() + "\n"
                + "Description: " + request.getDescription() + "\n"
                + "Fields: " + (request.getFields() != null ? request.getFields() : "{}") + "\n"
                + "Department: " + request.getDepartmentName() + "\n"
                + "History: " + request.getApprovalHistory() + "\n\n"
                + "Provide: recommendation (APPROVE/REJECT/REQUEST_INFO), confidenceScore, reasoning, and any warnings.";
    }

    private String buildSummaryPrompt(Request request) {
        return "Summarize this request:\n\n"
                + "Title: " + request.getTitle() + "\n"
                + "Type: " + request.getRequestTypeName() + "\n"
                + "Status: " + request.getStatus() + "\n"
                + "Submitted by: " + request.getSubmittedByName() + "\n"
                + "Priority: " + request.getPriority() + "\n"
                + "Description: " + request.getDescription() + "\n"
                + "Fields: " + (request.getFields() != null ? request.getFields() : "{}") + "\n"
                + "Comments: " + (request.getComments() != null ? request.getComments().size() : 0) + " comments\n\n"
                + "Provide: summary, detailedAnalysis, confidenceScore, and reasoning.";
    }

    private String buildExplanationPrompt(WorkflowExecution exec, Workflow workflow, String stepId) {
        return "Explain the decision made in this workflow execution:\n\n"
                + "Workflow: " + (workflow != null ? workflow.getName() : "Unknown") + "\n"
                + "Execution Status: " + exec.getStatus() + "\n"
                + "Triggered by: " + exec.getTriggeredBy() + "\n"
                + "Started: " + exec.getStartedAt() + "\n"
                + "Completed: " + exec.getCompletedAt() + "\n"
                + "Duration: " + exec.getTotalDurationMs() + "ms\n"
                + "Error: " + exec.getErrorMessage() + "\n"
                + (stepId != null ? "Step ID: " + stepId + "\n" : "")
                + "Execution Log: " + (exec.getExecutionLog() != null ? exec.getExecutionLog() : "[]") + "\n\n"
                + "Provide: summary, detailedAnalysis, confidenceScore, reasoning, and supporting evidence.";
    }

    private String buildPredictionPrompt(Request request, List<Request> history) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Predict the outcome of this request based on historical data:\n\n")
                .append("Current Request:\n")
                .append("Title: ").append(request.getTitle()).append("\n")
                .append("Type: ").append(request.getRequestTypeName()).append("\n")
                .append("Priority: ").append(request.getPriority()).append("\n")
                .append("Department: ").append(request.getDepartmentName()).append("\n")
                .append("Fields: ").append(request.getFields()).append("\n\n")
                .append("Historical Requests (").append(history.size()).append("):\n");
        for (Request h : history) {
            prompt.append("- ").append(h.getTitle())
                    .append(" | Status: ").append(h.getStatus())
                    .append(" | Priority: ").append(h.getPriority())
                    .append("\n");
        }
        prompt.append("\nProvide: recommendation (PREDICTED_APPROVE/PREDICTED_REJECT/UNCERTAIN), confidenceScore, reasoning, metrics.");
        return prompt.toString();
    }

    private String buildApproverPrompt(Request request) {
        return "Recommend the best approver for this request:\n\n"
                + "Title: " + request.getTitle() + "\n"
                + "Type: " + request.getRequestTypeName() + "\n"
                + "Priority: " + request.getPriority() + "\n"
                + "Submitted by: " + request.getSubmittedByName() + "\n"
                + "Department: " + request.getDepartmentName() + "\n"
                + "Amount/Value: " + (request.getFields() != null ? request.getFields().get("amount") : "N/A") + "\n\n"
                + "Provide: recommendation (role or user identifier), confidenceScore, reasoning, alternatives.";
    }

    private String buildOptimizationPrompt(Workflow workflow, List<WorkflowExecution> executions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this workflow for optimization opportunities:\n\n")
                .append("Workflow: ").append(workflow.getName()).append("\n")
                .append("Steps: ").append(workflow.getSteps() != null ? workflow.getSteps().size() : 0).append("\n")
                .append("Version: ").append(workflow.getVersion()).append("\n")
                .append("Status: ").append(workflow.getStatus()).append("\n")
                .append("Category: ").append(workflow.getCategory()).append("\n\n")
                .append("Executions (").append(executions.size()).append("):\n");
        for (WorkflowExecution e : executions) {
            prompt.append("- ").append(e.getStatus())
                    .append(" | ").append(e.getTotalDurationMs()).append("ms")
                    .append(" | ").append(e.getTriggeredBy()).append("\n");
        }
        prompt.append("\nReturn JSON with insights array containing optimization suggestions.");
        return prompt.toString();
    }

    private String buildBottleneckPrompt(List<WorkflowExecution> executions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Detect bottlenecks from current workflow executions:\n\n");
        for (WorkflowExecution e : executions) {
            prompt.append("- ").append(e.getWorkflowName())
                    .append(" | Status: ").append(e.getStatus())
                    .append(" | Duration: ").append(e.getTotalDurationMs()).append("ms")
                    .append(" | Started: ").append(e.getStartedAt())
                    .append("\n");
        }
        prompt.append("\nReturn JSON with insights array containing bottleneck detections.");
        return prompt.toString();
    }

    private String buildInsightPrompt(Workflow workflow, List<WorkflowExecution> executions) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate insights from workflow data:\n\n")
                .append("Workflow: ").append(workflow.getName()).append("\n")
                .append("Category: ").append(workflow.getCategory()).append("\n")
                .append("Steps: ").append(workflow.getSteps() != null ? workflow.getSteps().size() : 0).append("\n\n")
                .append("Execution History:\n");
        for (WorkflowExecution e : executions) {
            prompt.append("- ").append(e.getStatus()).append(" | ")
                    .append(e.getTotalDurationMs()).append("ms | ")
                    .append(e.getTriggeredBy()).append("\n");
        }
        prompt.append("\nReturn JSON with insights array containing actionable insights.");
        return prompt.toString();
    }

    private String buildConversationalContext(AIQueryRequest query) {
        StringBuilder context = new StringBuilder();
        if (query.getContextId() != null) {
            if ("request".equals(query.getContextType())) {
                requestRepository.findById(query.getContextId()).ifPresent(r ->
                        context.append("Request: ").append(r.getTitle())
                                .append(" (").append(r.getStatus()).append(")\n"));
            } else if ("workflow".equals(query.getContextType())) {
                workflowRepository.findById(query.getContextId()).ifPresent(w ->
                        context.append("Workflow: ").append(w.getName()).append("\n"));
            } else if ("execution".equals(query.getContextType())) {
                executionRepository.findById(query.getContextId()).ifPresent(e ->
                        context.append("Execution: ").append(e.getWorkflowName())
                                .append(" (").append(e.getStatus()).append(")\n"));
            }
        }
        if (query.getAdditionalContext() != null) {
            context.append("Additional: ").append(query.getAdditionalContext());
        }
        return context.toString();
    }

    private String workflowToText(Workflow workflow) {
        return "Name: " + workflow.getName() + "\n"
                + "Version: " + workflow.getVersion() + "\n"
                + "Status: " + workflow.getStatus() + "\n"
                + "Category: " + workflow.getCategory() + "\n"
                + "Steps: " + (workflow.getSteps() != null ? workflow.getSteps().size() : 0) + "\n";
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        if (val instanceof String) {
            try { return Double.parseDouble((String) val); }
            catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            List<String> result = new ArrayList<>();
            for (Object item : list) result.add(item != null ? item.toString() : "");
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Map ? (Map<String, Object>) val : Map.of();
    }
}
