package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.FraudCategoryScore;
import com.aiflow.enterprise.enums.FraudRiskLevel;
import com.aiflow.enterprise.enums.FraudStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.springframework.data.mongodb.core.mapping.Document(collection = "fraud_checks")
public class FraudCheck extends BaseEntity {

    @Indexed
    private String requestId;

    private String requestType;

    @Indexed
    private String userId;

    private String userName;

    @Indexed
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

    @Builder.Default
    private boolean escalated = false;

    private Instant escalatedAt;

    private String modelVersion;

    private long responseTimeMs;

    private Map<String, Object> metadata;

    @Builder.Default
    private Instant checkedAt = Instant.now();

    @Builder.Default
    private int version = 1;
}
