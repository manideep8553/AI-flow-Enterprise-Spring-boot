package com.aiflow.enterprise.dto.request;

import com.aiflow.enterprise.enums.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerRequest {

    @NotBlank(message = "Trigger name is required")
    private String name;

    private String description;

    @NotNull(message = "Trigger type is required")
    private TriggerType type;

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    private Map<String, Object> config;

    private Boolean active;

    private String createdBy;
}
