package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.TaskPriority;
import com.aiflow.enterprise.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "tasks")
@CompoundIndex(def = "{'workflowId': 1, 'executionId': 1}")
public class Task extends BaseEntity {

    @Indexed
    private String workflowId;

    @Indexed
    private String executionId;

    @Indexed
    private String workflowStepId;

    private String name;

    private String description;

    @Indexed
    private String assignee;

    @Indexed
    private TaskStatus status;

    private TaskPriority priority;

    private Instant dueDate;

    private Instant completedAt;

    private String result;

    private String comments;

    private Map<String, Object> metadata;
}
