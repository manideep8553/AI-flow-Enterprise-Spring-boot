package com.aiflow.enterprise.org.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignationRequest {
    @NotBlank private String organizationId;
    @NotBlank private String title;
    private String description;
    private Integer level;
    private String grade;
    private List<String> skills;
    private String careerPath;
}
