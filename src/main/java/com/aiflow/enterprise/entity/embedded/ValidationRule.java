package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {
    private String ruleType;
    private String operator;
    private Object value;
    private String errorMessage;
    private String dependsOnField;
    private Object dependsOnValue;
}
