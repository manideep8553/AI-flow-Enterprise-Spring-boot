package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.FraudCheckRequest;
import com.aiflow.enterprise.dto.request.FraudReviewRequest;
import com.aiflow.enterprise.dto.response.FraudAlertResponse;
import com.aiflow.enterprise.dto.response.FraudCheckResponse;
import com.aiflow.enterprise.dto.response.FraudRuleResponse;
import com.aiflow.enterprise.entity.FraudAlert;
import com.aiflow.enterprise.entity.FraudCheck;
import com.aiflow.enterprise.entity.FraudRule;
import com.aiflow.enterprise.entity.embedded.FraudCategoryScore;
import com.aiflow.enterprise.enums.FraudCategory;
import com.aiflow.enterprise.enums.FraudRiskLevel;
import com.aiflow.enterprise.enums.FraudStatus;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.FraudMapper;
import com.aiflow.enterprise.repository.FraudAlertRepository;
import com.aiflow.enterprise.repository.FraudCheckRepository;
import com.aiflow.enterprise.repository.FraudRuleRepository;
import com.aiflow.enterprise.service.FraudDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private static final Logger log = LoggerFactory.getLogger(FraudDetectionServiceImpl.class);

    private final FraudMLClient fraudMLClient;
    private final FraudCheckRepository fraudCheckRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final FraudMapper fraudMapper;

    public FraudDetectionServiceImpl(FraudMLClient fraudMLClient,
                                     FraudCheckRepository fraudCheckRepository,
                                     FraudAlertRepository fraudAlertRepository,
                                     FraudRuleRepository fraudRuleRepository,
                                     FraudMapper fraudMapper) {
        this.fraudMLClient = fraudMLClient;
        this.fraudCheckRepository = fraudCheckRepository;
        this.fraudAlertRepository = fraudAlertRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.fraudMapper = fraudMapper;
    }

    @Override
    public FraudCheckResponse analyzeFraud(FraudCheckRequest request) {
        FraudCheckResponse mlResponse = fraudMLClient.analyzeFraud(request);

        FraudCheck check = FraudCheck.builder()
                .requestId(request.getRequestId())
                .requestType(request.getRequestType())
                .userId(request.getUserId())
                .userName(request.getUserName())
                .department(request.getDepartment())
                .claimAmount(request.getClaimAmount())
                .category(request.getCategory())
                .vendor(request.getVendor())
                .description(request.getDescription())
                .overallRiskScore(mlResponse.getOverallRiskScore())
                .riskLevel(mlResponse.getRiskLevel())
                .status(FraudStatus.PENDING_REVIEW)
                .categoryScores(mlResponse.getCategoryScores())
                .explanation(mlResponse.getExplanation())
                .modelVersion(mlResponse.getModelVersion())
                .responseTimeMs(mlResponse.getResponseTimeMs())
                .metadata(buildMetadata(request))
                .checkedAt(Instant.now())
                .build();

        FraudCheck saved = fraudCheckRepository.save(check);

        if (saved.getRiskLevel() == FraudRiskLevel.HIGH || saved.getRiskLevel() == FraudRiskLevel.CRITICAL) {
            createAlert(saved);
        }

        log.info("Fraud check completed: id={} risk={} score={}",
                saved.getId(), saved.getRiskLevel(), saved.getOverallRiskScore());

        return fraudMapper.toCheckResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudCheckResponse getFraudCheckById(String id) {
        FraudCheck check = fraudCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FraudCheck", "id", id));
        return fraudMapper.toCheckResponse(check);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudCheckResponse> getAllFraudChecks(int page, int size, String riskLevel,
                                                       String status, String userId,
                                                       String department, Boolean escalated) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "checkedAt"));

        Page<FraudCheck> checks;
        if (riskLevel != null) {
            checks = fraudCheckRepository.findByRiskLevel(
                    FraudRiskLevel.valueOf(riskLevel.toUpperCase()), pageable);
        } else if (status != null) {
            checks = fraudCheckRepository.findByStatus(
                    FraudStatus.valueOf(status.toUpperCase()), pageable);
        } else if (userId != null) {
            checks = fraudCheckRepository.findByUserId(userId, pageable);
        } else if (department != null) {
            checks = fraudCheckRepository.findByDepartment(department, pageable);
        } else if (Boolean.TRUE.equals(escalated)) {
            checks = fraudCheckRepository.findByEscalatedTrue(pageable);
        } else {
            checks = fraudCheckRepository.findAll(pageable);
        }

        return checks.map(fraudMapper::toCheckResponse);
    }

    @Override
    public FraudCheckResponse reviewFraudCheck(FraudReviewRequest request, String reviewerId) {
        FraudCheck check = fraudCheckRepository.findById(request.getFraudCheckId())
                .orElseThrow(() -> new ResourceNotFoundException("FraudCheck", "id", request.getFraudCheckId()));

        check.setStatus(FraudStatus.valueOf(request.getStatus().toUpperCase()));
        check.setReviewedBy(reviewerId);
        check.setReviewedAt(Instant.now());
        check.setReviewNotes(request.getReviewNotes());

        if (request.getAssignedTo() != null) {
            check.setMetadata(new HashMap<>());
            check.getMetadata().put("assignedTo", request.getAssignedTo());
        }

        FraudCheck saved = fraudCheckRepository.save(check);
        log.info("Fraud check reviewed: id={} status={} by={}", saved.getId(), saved.getStatus(), reviewerId);
        return fraudMapper.toCheckResponse(saved);
    }

    @Override
    public FraudCheckResponse escalateFraudCheck(String id) {
        FraudCheck check = fraudCheckRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FraudCheck", "id", id));

        check.setEscalated(true);
        check.setEscalatedAt(Instant.now());
        check.setStatus(FraudStatus.UNDER_REVIEW);

        FraudCheck saved = fraudCheckRepository.save(check);

        createAlert(saved);

        log.warn("Fraud check escalated: id={} risk={}", saved.getId(), saved.getRiskLevel());
        return fraudMapper.toCheckResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudAlertResponse> getAlerts(int page, int size, String riskLevel,
                                               Boolean resolved, Boolean acknowledged,
                                               String userId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "alertedAt"));

        Page<FraudAlert> alerts;
        if (riskLevel != null) {
            alerts = fraudAlertRepository.findByRiskLevel(
                    FraudRiskLevel.valueOf(riskLevel.toUpperCase()), pageable);
        } else if (Boolean.TRUE.equals(resolved)) {
            alerts = fraudAlertRepository.findByResolvedFalse(pageable);
        } else if (Boolean.FALSE.equals(acknowledged)) {
            alerts = fraudAlertRepository.findByAcknowledgedFalse(pageable);
        } else if (userId != null) {
            alerts = fraudAlertRepository.findByUserId(userId, pageable);
        } else {
            alerts = fraudAlertRepository.findAll(pageable);
        }

        return alerts.map(fraudMapper::toAlertResponse);
    }

    @Override
    public FraudAlertResponse acknowledgeAlert(String alertId, String userId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(userId);
        FraudAlert saved = fraudAlertRepository.save(alert);
        return fraudMapper.toAlertResponse(saved);
    }

    @Override
    public FraudAlertResponse resolveAlert(String alertId, String userId) {
        FraudAlert alert = fraudAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudAlert", "id", alertId));
        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setAcknowledged(true);
        alert.setAcknowledgedBy(userId);
        FraudAlert saved = fraudAlertRepository.save(alert);
        return fraudMapper.toAlertResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFraudStatistics(String department, String timeframe) {
        Map<String, Object> stats = new HashMap<>();

        Instant since;
        if (timeframe != null) {
            since = switch (timeframe.toLowerCase()) {
                case "24h" -> Instant.now().minus(24, ChronoUnit.HOURS);
                case "7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
                case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
                case "90d" -> Instant.now().minus(90, ChronoUnit.DAYS);
                default -> Instant.now().minus(30, ChronoUnit.DAYS);
            };
        } else {
            since = Instant.now().minus(30, ChronoUnit.DAYS);
        }

        long totalChecks = department != null
                ? fraudCheckRepository.countByDepartmentAndCheckedAtAfter(department, since)
                : fraudCheckRepository.count();

        long criticalCount = fraudCheckRepository.countByRiskLevel(FraudRiskLevel.CRITICAL);
        long highCount = fraudCheckRepository.countByRiskLevel(FraudRiskLevel.HIGH);
        long mediumCount = fraudCheckRepository.countByRiskLevel(FraudRiskLevel.MEDIUM);
        long lowCount = fraudCheckRepository.countByRiskLevel(FraudRiskLevel.LOW);
        long pendingReview = fraudCheckRepository.countByStatus(FraudStatus.PENDING_REVIEW);
        long escalated = fraudCheckRepository.countByEscalatedTrue();
        long openAlerts = fraudAlertRepository.countByResolvedFalse();

        stats.put("totalChecks", totalChecks);
        stats.put("criticalCount", criticalCount);
        stats.put("highCount", highCount);
        stats.put("mediumCount", mediumCount);
        stats.put("lowCount", lowCount);
        stats.put("pendingReview", pendingReview);
        stats.put("escalated", escalated);
        stats.put("openAlerts", openAlerts);
        stats.put("department", department);
        stats.put("timeframe", timeframe != null ? timeframe : "all");

        return stats;
    }

    @Override
    public Map<String, Object> checkVendorRisk(String vendorName, String vendorId) {
        return fraudMLClient.checkVendorRisk(vendorName, vendorId, 0, 0, 0);
    }

    @Override
    public boolean isMlServiceAvailable() {
        return fraudMLClient.isServiceAvailable();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FraudRuleResponse> getEnabledRules() {
        return fraudRuleRepository.findByEnabledTrueOrderByPriorityDesc()
                .stream()
                .map(fraudMapper::toRuleResponse)
                .toList();
    }

    @Override
    public FraudRuleResponse createRule(Map<String, Object> ruleData) {
        FraudRule rule = FraudRule.builder()
                .ruleName((String) ruleData.get("ruleName"))
                .description((String) ruleData.get("description"))
                .category(ruleData.get("category") != null
                        ? FraudCategory.valueOf(((String) ruleData.get("category")).toUpperCase())
                        : null)
                .severity((String) ruleData.get("severity"))
                .enabled(ruleData.get("enabled") == null || Boolean.TRUE.equals(ruleData.get("enabled")))
                .config(ruleData.get("config") != null
                        ? (Map<String, Object>) ruleData.get("config")
                        : null)
                .condition((String) ruleData.get("condition"))
                .defaultScore(ruleData.get("defaultScore") != null
                        ? ((Number) ruleData.get("defaultScore")).doubleValue()
                        : 0.0)
                .action((String) ruleData.get("action"))
                .priority(ruleData.get("priority") != null
                        ? ((Number) ruleData.get("priority")).intValue()
                        : 0)
                .build();

        FraudRule saved = fraudRuleRepository.save(rule);
        return fraudMapper.toRuleResponse(saved);
    }

    @Override
    public FraudRuleResponse updateRule(String ruleId, Map<String, Object> ruleData) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudRule", "id", ruleId));

        if (ruleData.containsKey("ruleName")) rule.setRuleName((String) ruleData.get("ruleName"));
        if (ruleData.containsKey("description")) rule.setDescription((String) ruleData.get("description"));
        if (ruleData.containsKey("category")) rule.setCategory(
                FraudCategory.valueOf(((String) ruleData.get("category")).toUpperCase()));
        if (ruleData.containsKey("severity")) rule.setSeverity((String) ruleData.get("severity"));
        if (ruleData.containsKey("enabled")) rule.setEnabled(Boolean.TRUE.equals(ruleData.get("enabled")));
        if (ruleData.containsKey("config")) rule.setConfig((Map<String, Object>) ruleData.get("config"));
        if (ruleData.containsKey("condition")) rule.setCondition((String) ruleData.get("condition"));
        if (ruleData.containsKey("defaultScore")) rule.setDefaultScore(
                ((Number) ruleData.get("defaultScore")).doubleValue());
        if (ruleData.containsKey("action")) rule.setAction((String) ruleData.get("action"));
        if (ruleData.containsKey("priority")) rule.setPriority(
                ((Number) ruleData.get("priority")).intValue());

        FraudRule saved = fraudRuleRepository.save(rule);
        return fraudMapper.toRuleResponse(saved);
    }

    @Override
    public void deleteRule(String ruleId) {
        FraudRule rule = fraudRuleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("FraudRule", "id", ruleId));
        fraudRuleRepository.delete(rule);
    }

    private void createAlert(FraudCheck check) {
        try {
            List<String> flaggedCategories = check.getCategoryScores() != null
                    ? check.getCategoryScores().stream()
                    .filter(s -> s.getScore() >= 0.5)
                    .map(FraudCategoryScore::getCategory)
                    .toList()
                    : List.of();

            FraudAlert alert = FraudAlert.builder()
                    .fraudCheckId(check.getId())
                    .requestId(check.getRequestId())
                    .userId(check.getUserId())
                    .userName(check.getUserName())
                    .department(check.getDepartment())
                    .claimAmount(check.getClaimAmount())
                    .riskLevel(check.getRiskLevel())
                    .flaggedCategories(flaggedCategories)
                    .summary(String.format("Fraud alert: %s risk (score: %.2f) - %d categories flagged",
                            check.getRiskLevel(), check.getOverallRiskScore(), flaggedCategories.size()))
                    .build();

            fraudAlertRepository.save(alert);
            log.info("Fraud alert created: checkId={} risk={}", check.getId(), check.getRiskLevel());
        } catch (Exception e) {
            log.warn("Failed to create fraud alert: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildMetadata(FraudCheckRequest request) {
        Map<String, Object> meta = new HashMap<>();
        if (request.getReceiptText() != null) meta.put("hasReceiptText", true);
        if (request.getReceiptHash() != null) meta.put("hasReceiptHash", true);
        if (request.getItems() != null) meta.put("itemCount", request.getItems().size());
        if (request.getPolicyRules() != null) meta.put("hasPolicyRules", true);
        return meta;
    }
}
