package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.entity.embedded.FraudCategoryScore;
import com.aiflow.enterprise.enums.FraudRiskLevel;
import com.aiflow.enterprise.enums.FraudStatus;
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
public class FraudCheckResponse {

    private String id;
    private String requestId;
    private String requestType;
    private String userId;
    private String userName;
    private String department;
    private double claimAmount;
    private String category;
    private String vendor;
    private String description;
    private double overallRiskScore;
    private FraudRiskLevel riskLevel;
    private FraudStatus status;
    private List<FraudCategoryScore> categoryScores;
    private String explanation;
    private String aiRecommendation;
    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewNotes;
    private boolean escalated;
    private Instant escalatedAt;
    private String modelVersion;
    private long responseTimeMs;
    private Map<String, Object> metadata;
    private Instant checkedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
