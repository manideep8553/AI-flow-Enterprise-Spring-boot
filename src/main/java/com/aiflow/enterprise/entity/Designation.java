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

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "designations")
@CompoundIndex(def = "{'organizationId': 1, 'title': 1}", unique = true)
public class Designation extends BaseEntity {

    @Indexed
    private String organizationId;

    @Indexed
    private String title;

    private String description;

    private Integer level;

    private String grade;

    private List<String> skills;

    private String careerPath;

    @Builder.Default
    private boolean active = true;
}
