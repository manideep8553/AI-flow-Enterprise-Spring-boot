package com.aiflow.enterprise.org.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationRequest {
    @NotBlank private String name;
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
    private String description;
    private String industry;
    private List<String> domains;
    private Map<String, Object> metadata;
}
