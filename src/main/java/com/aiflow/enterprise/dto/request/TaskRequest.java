package com.aiflow.enterprise.dto.request;

import com.aiflow.enterprise.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Workflow ID is required")
    private String workflowId;

    @NotBlank(message = "Execution ID is required")
    private String executionId;

    private String workflowStepId;

    @NotBlank(message = "Task name is required")
    private String name;

    private String description;

    private String assignee;

    @NotNull(message = "Task status is required")
    private com.aiflow.enterprise.enums.TaskStatus status;

    private TaskPriority priority;

    private Instant dueDate;

    private Map<String, Object> metadata;
}
