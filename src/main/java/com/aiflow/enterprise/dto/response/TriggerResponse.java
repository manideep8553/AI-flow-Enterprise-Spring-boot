package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.enums.TriggerType;
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
public class TriggerResponse {

    private String id;
    private String name;
    private String description;
    private TriggerType type;
    private String workflowId;
    private String workflowName;
    private Map<String, Object> config;
    private Boolean active;
    private Instant lastTriggeredAt;
    private Long triggerCount;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
