package com.aiflow.enterprise.dto.request.step;

import com.aiflow.enterprise.enums.StepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class WorkflowStepRequest {

    @NotBlank(message = "Step name is required")
    private String name;

    private String description;

    @NotNull(message = "Step type is required")
    private StepType type;

    @NotNull(message = "Step order is required")
    @PositiveOrZero(message = "Step order must be zero or positive")
    private Integer order;

    private Map<String, Object> config;

    private List<String> dependsOn;

    private Integer timeoutSeconds;

    private Boolean mandatory;
}
