package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCategoryScore {
    private String category;
    private double score;
    private double confidence;
    private String explanation;
    private Map<String, Object> details;
}
