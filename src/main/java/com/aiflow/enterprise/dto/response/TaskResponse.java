package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.enums.TaskPriority;
import com.aiflow.enterprise.enums.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResponse {

    private String id;
    private String workflowId;
    private String executionId;
    private String workflowStepId;
    private String name;
    private String description;
    private String assignee;
    private TaskStatus status;
    private TaskPriority priority;
    private Instant dueDate;
    private Instant completedAt;
    private String result;
    private String comments;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
