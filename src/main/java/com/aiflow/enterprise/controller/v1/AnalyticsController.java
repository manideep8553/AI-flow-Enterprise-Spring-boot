package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.AnalyticsSummaryResponse;
import com.aiflow.enterprise.dto.response.AnalyticsSummaryResponse.*;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Enterprise analytics and business intelligence APIs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get complete platform analytics summary with all KPI metrics")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getSummary(
            @RequestParam(defaultValue = "30d") String period,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String workflowId) {
        AnalyticsSummaryResponse summary = analyticsService.getSummary(period, department, workflowId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/workflows")
    @Operation(summary = "Get workflow execution analytics and trends")
    public ResponseEntity<ApiResponse<AnalyticsSummaryResponse>> getWorkflows(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWorkflowAnalytics(period)));
    }

    @GetMapping("/workflows/metrics")
    @Operation(summary = "Get workflow execution metrics")
    public ResponseEntity<ApiResponse<WorkflowMetrics>> getWorkflowMetrics(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getWorkflowMetrics(period)));
    }

    @GetMapping("/requests")
    @Operation(summary = "Get request analytics with approval trends and SLA compliance")
    public ResponseEntity<ApiResponse<RequestMetrics>> getRequests(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRequestMetrics(period)));
    }

    @GetMapping("/users")
    @Operation(summary = "Get user activity and adoption metrics")
    public ResponseEntity<ApiResponse<UserMetrics>> getUsers(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getUserMetrics(period)));
    }

    @GetMapping("/documents")
    @Operation(summary = "Get document and storage usage analytics")
    public ResponseEntity<ApiResponse<DocumentMetrics>> getDocuments() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDocumentMetrics()));
    }

    @GetMapping("/fraud")
    @Operation(summary = "Get fraud detection analytics and risk metrics")
    public ResponseEntity<ApiResponse<FraudMetrics>> getFraud(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getFraudMetrics(period)));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notification delivery analytics")
    public ResponseEntity<ApiResponse<NotificationMetrics>> getNotifications(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getNotificationMetrics(period)));
    }

    @GetMapping("/ai")
    @Operation(summary = "Get AI service usage and effectiveness metrics")
    public ResponseEntity<ApiResponse<AIMetrics>> getAI(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAIMetrics(period)));
    }

    @GetMapping("/scheduler")
    @Operation(summary = "Get scheduler job health and execution metrics")
    public ResponseEntity<ApiResponse<SchedulerMetrics>> getScheduler() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getSchedulerMetrics()));
    }

    @GetMapping("/bottlenecks")
    @Operation(summary = "Detect workflow bottlenecks and performance issues")
    public ResponseEntity<ApiResponse<List<BottleneckItem>>> getBottlenecks() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getBottlenecks()));
    }

    @GetMapping("/departments")
    @Operation(summary = "Get department productivity and performance metrics")
    public ResponseEntity<ApiResponse<List<DepartmentMetric>>> getDepartments(
            @RequestParam(defaultValue = "30d") String period) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDepartmentMetrics(period)));
    }

    @GetMapping("/kpi")
    @Operation(summary = "Get top-level KPI metrics for dashboard cards")
    public ResponseEntity<ApiResponse<KPIMetrics>> getKPI(
            @RequestParam(defaultValue = "30d") String period) {
        AnalyticsSummaryResponse summary = analyticsService.getSummary(period, null, null);
        return ResponseEntity.ok(ApiResponse.success(summary.getKpi()));
    }

    @GetMapping(value = "/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to real-time analytics updates via SSE")
    public SseEmitter streamRealtimeAnalytics(
            @RequestParam(defaultValue = "30d") String period) {
        SseEmitter emitter = new SseEmitter(Duration.ofHours(1).toMillis());
        sseEmitters.add(emitter);

        emitter.onCompletion(() -> sseEmitters.remove(emitter));
        emitter.onTimeout(() -> sseEmitters.remove(emitter));
        emitter.onError(e -> sseEmitters.remove(emitter));

        sendInitialAnalytics(emitter, period);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                AnalyticsSummaryResponse summary = analyticsService.getSummary(period, null, null);
                emitter.send(SseEmitter.event()
                        .name("analytics-update")
                        .data(summary));
            } catch (IOException e) {
                sseEmitters.remove(emitter);
                executor.shutdown();
            }
        }, 30, 30, TimeUnit.SECONDS);

        executor.schedule(() -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {}
        }, 55, TimeUnit.MINUTES);

        return emitter;
    }

    private void sendInitialAnalytics(SseEmitter emitter, String period) {
        try {
            AnalyticsSummaryResponse summary = analyticsService.getSummary(period, null, null);
            emitter.send(SseEmitter.event()
                    .name("initial")
                    .data(summary));
        } catch (IOException e) {
            sseEmitters.remove(emitter);
        }
    }

    @PostMapping("/evict-cache")
    @Operation(summary = "Evict analytics cache (admin)")
    public ResponseEntity<ApiResponse<String>> evictCache() {
        analyticsService.evictAllCache();
        return ResponseEntity.ok(ApiResponse.success("Analytics cache evicted"));
    }
}
