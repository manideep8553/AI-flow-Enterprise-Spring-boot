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
public class DepartmentRequest {
    @NotBlank private String organizationId;
    @NotBlank private String name;
    private String code;
    private String description;
    private String headEmployeeId;
    private String parentDepartmentId;
    private String costCenter;
    private String email;
    private String phone;
    private String location;
    private Map<String, Object> metadata;
}
