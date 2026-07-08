package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.ai.ConfidenceLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponse {

    private String id;

    private String type;

    private String summary;

    private String detailedAnalysis;

    private String recommendation;

    private ConfidenceLevel confidenceLevel;

    private double confidenceScore;

    private String reasoning;

    private List<String> alternatives;

    private Map<String, Object> data;

    private Map<String, Double> metrics;

    private List<String> warnings;

    private String modelUsed;

    private int promptTokens;

    private int completionTokens;

    private long responseTimeMs;
}
