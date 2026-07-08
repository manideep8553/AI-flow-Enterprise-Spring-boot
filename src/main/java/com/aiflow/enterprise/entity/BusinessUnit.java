package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "business_units")
public class BusinessUnit extends BaseEntity {

    @Indexed
    private String organizationId;

    @Indexed
    private String name;

    private String code;

    private String description;

    private String headEmployeeId;

    private String budgetCode;

    @Builder.Default
    private boolean active = true;

    private Map<String, Object> metadata;
}
