package com.aiflow.enterprise.org.dto.response;

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
public class TeamResponse {
    private String id;
    private String organizationId;
    private String departmentId;
    private String name;
    private String description;
    private String leadEmployeeId;
    private boolean active;
    private String email;
    private String slackChannel;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
