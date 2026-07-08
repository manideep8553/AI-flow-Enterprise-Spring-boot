package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.enums.FraudRiskLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudAlertResponse {

    private String id;
    private String fraudCheckId;
    private String requestId;
    private String userId;
    private String userName;
    private String department;
    private double claimAmount;
    private FraudRiskLevel riskLevel;
    private List<String> flaggedCategories;
    private String summary;
    private boolean acknowledged;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    private boolean resolved;
    private Instant resolvedAt;
    private String assignedTo;
    private Instant alertedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
