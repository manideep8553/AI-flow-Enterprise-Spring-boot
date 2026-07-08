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
@Document(collection = "teams")
@CompoundIndex(def = "{'departmentId': 1, 'name': 1}", unique = true)
public class Team extends BaseEntity {

    @Indexed
    private String organizationId;

    @Indexed
    private String departmentId;

    @Indexed
    private String name;

    private String description;

    private String leadEmployeeId;

    @Builder.Default
    private boolean active = true;

    private String email;

    private String slackChannel;

    private Map<String, Object> metadata;
}
