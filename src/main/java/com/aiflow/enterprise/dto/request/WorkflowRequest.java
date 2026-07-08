package com.aiflow.enterprise.dto.request;

import com.aiflow.enterprise.dto.request.step.WorkflowStepRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class WorkflowRequest {

    @NotBlank(message = "Workflow name is required")
    @Size(max = 255, message = "Workflow name must not exceed 255 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Valid
    private List<WorkflowStepRequest> steps;

    private Map<String, Object> metadata;

    private List<String> tags;

    private String category;
}
