package com.aiflow.enterprise.org.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {
    @NotBlank private String organizationId;
    @NotBlank private String departmentId;
    @NotBlank private String name;
    private String description;
    private String leadEmployeeId;
    private String email;
    private String slackChannel;
    private Map<String, Object> metadata;
}
