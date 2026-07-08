package com.aiflow.enterprise.org.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DesignationResponse {
    private String id;
    private String organizationId;
    private String title;
    private String description;
    private Integer level;
    private String grade;
    private List<String> skills;
    private String careerPath;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
