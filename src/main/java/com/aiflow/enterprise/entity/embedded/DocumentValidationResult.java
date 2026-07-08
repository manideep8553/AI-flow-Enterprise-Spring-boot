package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationResult {
    private String ruleName;
    private boolean passed;
    private String severity;
    private String message;
    private String fieldName;
    private String expectedValue;
    private String actualValue;
    private List<String> suggestions;
}
