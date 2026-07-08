package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.enums.FraudCategory;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FraudRuleResponse {

    private String id;
    private String ruleName;
    private String description;
    private FraudCategory category;
    private String severity;
    private boolean enabled;
    private Map<String, Object> config;
    private String condition;
    private double defaultScore;
    private String action;
    private int priority;
    private int version;
}
