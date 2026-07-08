package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "workflow_templates")
public class WorkflowTemplate extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    private String category;

    private List<String> tags;

    private List<WorkflowStep> steps;

    private Map<String, Object> metadata;

    @Builder.Default
    private boolean published = false;

    private Long usageCount;

    private String createdBy;

    private String icon;
}
