package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.FraudCheckRequest;
import com.aiflow.enterprise.dto.request.FraudReviewRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.FraudAlertResponse;
import com.aiflow.enterprise.dto.response.FraudCheckResponse;
import com.aiflow.enterprise.dto.response.FraudRuleResponse;
import com.aiflow.enterprise.service.FraudDetectionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud")
public class FraudDetectionController {

    private final FraudDetectionService fraudDetectionService;

    public FraudDetectionController(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @PostMapping("/analyze")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> analyzeFraud(
            @Valid @RequestBody FraudCheckRequest request) {
        FraudCheckResponse response = fraudDetectionService.analyzeFraud(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Fraud analysis completed"));
    }

    @GetMapping("/checks/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'AUDITOR')")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> getFraudCheck(@PathVariable String id) {
        FraudCheckResponse response = fraudDetectionService.getFraudCheckById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/checks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Page<FraudCheckResponse>>> getAllFraudChecks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Boolean escalated) {
        Page<FraudCheckResponse> checks = fraudDetectionService.getAllFraudChecks(
                page, size, riskLevel, status, userId, department, escalated);
        return ResponseEntity.ok(ApiResponse.success(checks));
    }

    @PostMapping("/checks/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> reviewFraudCheck(
            @PathVariable String id,
            @Valid @RequestBody FraudReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        request.setFraudCheckId(id);
        FraudCheckResponse response = fraudDetectionService.reviewFraudCheck(
                request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Fraud check reviewed"));
    }

    @PostMapping("/checks/{id}/escalate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<FraudCheckResponse>> escalateFraudCheck(
            @PathVariable String id) {
        FraudCheckResponse response = fraudDetectionService.escalateFraudCheck(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Fraud check escalated"));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Page<FraudAlertResponse>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) Boolean acknowledged,
            @RequestParam(required = false) String userId) {
        Page<FraudAlertResponse> alerts = fraudDetectionService.getAlerts(
                page, size, riskLevel, resolved, acknowledged, userId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping("/alerts/{id}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> acknowledgeAlert(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        FraudAlertResponse response = fraudDetectionService.acknowledgeAlert(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Alert acknowledged"));
    }

    @PostMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<ApiResponse<FraudAlertResponse>> resolveAlert(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        FraudAlertResponse response = fraudDetectionService.resolveAlert(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "Alert resolved"));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String timeframe) {
        Map<String, Object> stats = fraudDetectionService.getFraudStatistics(department, timeframe);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/vendor-risk")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'FINANCE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkVendorRisk(
            @RequestParam String vendorName,
            @RequestParam(required = false) String vendorId) {
        Map<String, Object> result = fraudDetectionService.checkVendorRisk(vendorName, vendorId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mlServiceHealth() {
        boolean available = fraudDetectionService.isMlServiceAvailable();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "service", "fraud-ml",
                "available", available
        )));
    }

    @GetMapping("/rules")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<FraudRuleResponse>>> getEnabledRules() {
        List<FraudRuleResponse> rules = fraudDetectionService.getEnabledRules();
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudRuleResponse>> createRule(
            @RequestBody Map<String, Object> ruleData) {
        FraudRuleResponse response = fraudDetectionService.createRule(ruleData);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Fraud rule created"));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FraudRuleResponse>> updateRule(
            @PathVariable String id,
            @RequestBody Map<String, Object> ruleData) {
        FraudRuleResponse response = fraudDetectionService.updateRule(id, ruleData);
        return ResponseEntity.ok(ApiResponse.success(response, "Fraud rule updated"));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable String id) {
        fraudDetectionService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }
}
