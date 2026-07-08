package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "organizations")
public class Organization extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String legalName;

    @Indexed(unique = true)
    private String registrationNumber;

    private String taxId;

    @Indexed
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

    @Builder.Default
    private boolean active = true;

    private String createdBy;

    private List<String> domains;

    private Map<String, Object> metadata;
}
