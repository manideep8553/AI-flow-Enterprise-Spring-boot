package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.FraudCheckRequest;
import com.aiflow.enterprise.dto.response.FraudCheckResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FraudMLClient {

    private static final Logger log = LoggerFactory.getLogger(FraudMLClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fraud.ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    public FraudMLClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public FraudCheckResponse analyzeFraud(FraudCheckRequest request) {
        try {
            Map<String, Object> payload = buildPayload(request);
            String url = mlServiceUrl + "/analyze";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            long start = System.currentTimeMillis();
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            long elapsed = System.currentTimeMillis() - start;

            if (response.getBody() == null) {
                log.warn("Empty response from ML service");
                return fallbackResponse(request, "ML service returned empty response");
            }

            FraudCheckResponse result = objectMapper.treeToValue(response.getBody(), FraudCheckResponse.class);
            log.info("Fraud ML analysis completed in {}ms - risk: {}",
                    elapsed, result != null ? result.getRiskLevel() : "unknown");
            return result;

        } catch (RestClientException e) {
            log.error("ML service call failed: {}", e.getMessage());
            return fallbackResponse(request, "ML service unavailable: " + e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse ML service response: {}", e.getMessage());
            return fallbackResponse(request, "Failed to parse ML response: " + e.getMessage());
        }
    }

    public Map<String, Object> checkVendorRisk(String vendorName, String vendorId,
                                                int transactionCount, double totalAmount,
                                                double averageAmount) {
        try {
            String url = mlServiceUrl + "/vendor/risk";
            Map<String, Object> payload = new HashMap<>();
            payload.put("vendorName", vendorName);
            payload.put("vendorId", vendorId);
            payload.put("transactionCount", transactionCount);
            payload.put("totalAmount", totalAmount);
            payload.put("averageAmount", averageAmount);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            if (response.getBody() != null) {
                return objectMapper.treeToValue(response.getBody(), new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("Vendor risk check failed: {}", e.getMessage());
        }
        return Map.of("vendorName", vendorName, "riskScore", 0.0, "riskLevel", "LOW");
    }

    public List<FraudCheckResponse> analyzeBatch(List<FraudCheckRequest> requests) {
        try {
            String url = mlServiceUrl + "/analyze/batch";
            List<Map<String, Object>> payloads = requests.stream()
                    .map(this::buildPayload)
                    .toList();

            Map<String, Object> batchPayload = new HashMap<>();
            batchPayload.put("claims", payloads);
            batchPayload.put("history", Collections.emptyList());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(batchPayload, headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(url, entity, JsonNode.class);
            if (response.getBody() != null && response.getBody().isArray()) {
                return objectMapper.readValue(
                        response.getBody().traverse(),
                        new TypeReference<List<FraudCheckResponse>>() {});
            }
        } catch (Exception e) {
            log.error("Batch fraud analysis failed: {}", e.getMessage());
        }
        return requests.stream()
                .map(r -> fallbackResponse(r, "Batch ML service unavailable"))
                .toList();
    }

    public boolean isServiceAvailable() {
        try {
            String url = mlServiceUrl + "/health";
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(url, JsonNode.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> buildPayload(FraudCheckRequest req) {
        Map<String, Object> claim = new HashMap<>();
        claim.put("id", req.getRequestId());
        claim.put("userId", req.getUserId());
        claim.put("userName", req.getUserName());
        claim.put("department", req.getDepartment());
        claim.put("amount", req.getClaimAmount());
        claim.put("category", req.getCategory());
        claim.put("description", req.getDescription());
        claim.put("vendor", req.getVendor());
        claim.put("receiptText", req.getReceiptText());
        claim.put("receiptHash", req.getReceiptHash());

        if (req.getItems() != null) {
            claim.put("items", req.getItems().stream().map(item -> {
                Map<String, Object> m = new HashMap<>();
                m.put("amount", item.getAmount());
                m.put("category", item.getCategory());
                m.put("description", item.getDescription());
                m.put("date", item.getDate());
                m.put("vendor", item.getVendor());
                m.put("department", item.getDepartment());
                return m;
            }).toList());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("claim", claim);
        payload.put("policyRules", req.getPolicyRules() != null ? req.getPolicyRules() : Map.of());
        payload.put("history", Collections.emptyList());
        payload.put("userHistory", Collections.emptyList());
        return payload;
    }

    private FraudCheckResponse fallbackResponse(FraudCheckRequest req, String reason) {
        return FraudCheckResponse.builder()
                .requestId(req.getRequestId())
                .userId(req.getUserId())
                .userName(req.getUserName())
                .department(req.getDepartment())
                .claimAmount(req.getClaimAmount())
                .category(req.getCategory())
                .vendor(req.getVendor())
                .overallRiskScore(0.0)
                .riskLevel(com.aiflow.enterprise.enums.FraudRiskLevel.LOW)
                .explanation("Fraud check unavailable: " + reason)
                .modelVersion("fallback")
                .categoryScores(Collections.emptyList())
                .build();
    }
}
