package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedField {
    private String name;
    private String label;
    private String value;
    private String normalizedValue;
    private double confidence;
    private String source;
    private String pageNumber;
    private String boundingBox;
    private boolean validated;
    private String validationMessage;
    private String dataType;
}
