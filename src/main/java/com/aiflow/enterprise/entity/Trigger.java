package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.TriggerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "triggers")
public class Trigger extends BaseEntity {

    @Indexed
    private String name;

    private String description;

    @Indexed
    private TriggerType type;

    @Indexed
    private String workflowId;

    private String workflowName;

    private Map<String, Object> config;

    @Indexed
    private Boolean active;

    private Instant lastTriggeredAt;

    private Long triggerCount;

    private String createdBy;
}
