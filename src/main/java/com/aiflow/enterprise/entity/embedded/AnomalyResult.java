package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyResult {
    private String type;
    private String description;
    private double severityScore;
    private String fieldName;
    private String expectedValue;
    private String actualValue;
    private String recommendation;
}
