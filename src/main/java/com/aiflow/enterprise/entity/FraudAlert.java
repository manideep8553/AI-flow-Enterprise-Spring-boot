package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.FraudRiskLevel;
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

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.springframework.data.mongodb.core.mapping.Document(collection = "fraud_alerts")
public class FraudAlert extends BaseEntity {

    @Indexed
    private String fraudCheckId;

    @Indexed
    private String requestId;

    @Indexed
    private String userId;

    private String userName;

    private String department;

    private double claimAmount;

    @Indexed
    private FraudRiskLevel riskLevel;

    private List<String> flaggedCategories;

    private String summary;

    private boolean acknowledged;

    private Instant acknowledgedAt;

    private String acknowledgedBy;

    @Indexed
    private boolean resolved;

    private Instant resolvedAt;

    @Indexed
    private String assignedTo;

    @Builder.Default
    private Instant alertedAt = Instant.now();

    @Builder.Default
    private int version = 1;
}
