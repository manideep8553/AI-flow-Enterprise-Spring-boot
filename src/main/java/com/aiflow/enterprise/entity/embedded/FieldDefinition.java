package com.aiflow.enterprise.entity.embedded;

import com.aiflow.enterprise.enums.FieldType;
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
public class FieldDefinition {
    private String fieldKey;
    private String label;
    private String placeholder;
    private String helpText;
    private FieldType type;
    private boolean required;
    private boolean readOnly;
    private boolean unique;
    private Object defaultValue;
    private List<String> options;
    private Map<String, Object> optionLabels;
    private Integer minLength;
    private Integer maxLength;
    private Double minValue;
    private Double maxValue;
    private String regexPattern;
    private String regexMessage;
    private Integer order;
    private String sectionId;
    private String dependsOnField;
    private Object dependsOnValue;
    private List<ValidationRule> validationRules;
}
