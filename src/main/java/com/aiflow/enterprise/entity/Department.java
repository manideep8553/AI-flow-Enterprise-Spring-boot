package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "departments")
@CompoundIndex(def = "{'organizationId': 1, 'name': 1}", unique = true)
public class Department extends BaseEntity {

    @Indexed
    private String organizationId;

    @Indexed
    private String name;

    private String code;

    private String description;

    private String headEmployeeId;

    private String parentDepartmentId;

    private String costCenter;

    @Builder.Default
    private boolean active = true;

    private String email;

    private String phone;

    private String location;

    private Map<String, Object> metadata;
}
