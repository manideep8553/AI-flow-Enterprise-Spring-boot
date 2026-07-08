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
public class AIInsightResponse {

    private String type;

    private String title;

    private String description;

    private ConfidenceLevel confidenceLevel;

    private double confidenceScore;

    private String reasoning;

    private List<String> actionItems;

    private Map<String, Object> metrics;

    private String severity;

    private String category;
}
