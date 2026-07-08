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

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "workflows")
public class Workflow extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    @Indexed
    private Integer version;

    @Indexed
    private WorkflowStatus status;

    private List<WorkflowStep> steps;

    private Map<String, Object> metadata;

    @Indexed
    private List<String> tags;

    private String createdBy;

    private String category;

    private List<VersionSnapshot> versionHistory;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionSnapshot {
        private Integer version;
        private List<WorkflowStep> steps;
        private Map<String, Object> metadata;
        private Instant archivedAt;
        private String archivedBy;
    }
}
