package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.FraudCheckRequest;
import com.aiflow.enterprise.dto.request.FraudReviewRequest;
import com.aiflow.enterprise.dto.response.FraudAlertResponse;
import com.aiflow.enterprise.dto.response.FraudCheckResponse;
import com.aiflow.enterprise.dto.response.FraudRuleResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface FraudDetectionService {

    FraudCheckResponse analyzeFraud(FraudCheckRequest request);

    FraudCheckResponse getFraudCheckById(String id);

    Page<FraudCheckResponse> getAllFraudChecks(int page, int size, String riskLevel,
                                                String status, String userId,
                                                String department, Boolean escalated);

    FraudCheckResponse reviewFraudCheck(FraudReviewRequest request, String reviewerId);

    FraudCheckResponse escalateFraudCheck(String id);

    Page<FraudAlertResponse> getAlerts(int page, int size, String riskLevel,
                                        Boolean resolved, Boolean acknowledged,
                                        String userId);

    FraudAlertResponse acknowledgeAlert(String alertId, String userId);

    FraudAlertResponse resolveAlert(String alertId, String userId);

    Map<String, Object> getFraudStatistics(String department, String timeframe);

    Map<String, Object> checkVendorRisk(String vendorName, String vendorId);

    boolean isMlServiceAvailable();

    List<FraudRuleResponse> getEnabledRules();

    FraudRuleResponse createRule(Map<String, Object> ruleData);

    FraudRuleResponse updateRule(String ruleId, Map<String, Object> ruleData);

    void deleteRule(String ruleId);
}
