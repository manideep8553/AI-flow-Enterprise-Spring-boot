package com.aiflow.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckRequest {

    @NotBlank
    private String requestId;

    private String requestType;

    private String userId;

    private String userName;

    private String department;

    @Positive
    private double claimAmount;

    private String category;

    private String vendor;

    private String description;

    private String receiptText;

    private String receiptHash;

    private List<TransactionItem> items;

    private Map<String, Object> policyRules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {
        private double amount;
        private String category;
        private String description;
        private String date;
        private String vendor;
        private String department;
    }
}
