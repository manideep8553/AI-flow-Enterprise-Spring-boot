package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.dto.response.step.WorkflowStepResponse;
import com.aiflow.enterprise.enums.WorkflowStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowResponse {

    private String id;
    private String name;
    private String description;
    private Integer version;
    private WorkflowStatus status;
    private List<WorkflowStepResponse> steps;
    private Map<String, Object> metadata;
    private List<String> tags;
    private String createdBy;
    private String category;
    private Instant createdAt;
    private Instant updatedAt;
}
