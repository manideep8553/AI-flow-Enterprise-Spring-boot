package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateInfo {
    private boolean isDuplicate;
    private double matchScore;
    private String matchedDocumentId;
    private String matchedDocumentName;
    private String matchReason;
    private String matchedField;
    private String matchedValue;
}
