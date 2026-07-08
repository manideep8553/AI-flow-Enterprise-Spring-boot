package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.FraudCategory;
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
@org.springframework.data.mongodb.core.mapping.Document(collection = "fraud_rules")
public class FraudRule extends BaseEntity {

    @Indexed(unique = true)
    private String ruleName;

    private String description;

    @Indexed
    private FraudCategory category;

    private String severity;

    @Builder.Default
    private boolean enabled = true;

    private Map<String, Object> config;

    private String condition;

    private double defaultScore;

    private String action;

    @Builder.Default
    private int priority = 0;

    @Builder.Default
    private int version = 1;
}
