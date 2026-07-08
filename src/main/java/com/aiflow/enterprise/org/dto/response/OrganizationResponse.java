package com.aiflow.enterprise.org.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationResponse {
    private String id;
    private String name;
    private String legalName;
    private String registrationNumber;
    private String taxId;
    private String email;
    private String phone;
    private String website;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String logoUrl;
    private String description;
    private String industry;
    private Integer employeeCount;
    private boolean active;
    private String createdBy;
    private List<String> domains;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
