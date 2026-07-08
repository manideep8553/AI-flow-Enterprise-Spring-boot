package com.aiflow.enterprise.dto.response.step;

import com.aiflow.enterprise.enums.StepType;
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
public class WorkflowStepResponse {

    private String stepId;
    private String name;
    private String description;
    private StepType type;
    private Integer order;
    private Map<String, Object> config;
    private List<String> dependsOn;
    private Integer timeoutSeconds;
    private Boolean mandatory;
}
