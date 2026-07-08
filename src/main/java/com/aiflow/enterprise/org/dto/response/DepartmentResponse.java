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
public class DepartmentResponse {
    private String id;
    private String organizationId;
    private String name;
    private String code;
    private String description;
    private String headEmployeeId;
    private String parentDepartmentId;
    private String costCenter;
    private boolean active;
    private String email;
    private String phone;
    private String location;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
