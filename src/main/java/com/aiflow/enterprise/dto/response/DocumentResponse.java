package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.entity.embedded.AnomalyResult;
import com.aiflow.enterprise.entity.embedded.DocumentValidationResult;
import com.aiflow.enterprise.entity.embedded.DuplicateInfo;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentResponse {

    private String id;
    private String fileName;
    private String originalName;
    private String mimeType;
    private long fileSize;
    private DocumentType documentType;
    private double documentTypeConfidence;
    private ProcessingStatus processingStatus;
    private String uploadedBy;
    private Instant uploadedAt;
    private Instant processedAt;
    private String s3Url;
    private String ocrMethod;
    private List<ExtractedField> extractedFields;
    private Map<String, Object> extractedData;
    private List<DocumentValidationResult> validationResults;
    private List<AnomalyResult> anomalies;
    private DuplicateInfo duplicateInfo;
    private String summary;
    private String aiAnalysis;
    private String requestId;
    private String requestTypeId;
    private int pageCount;
    private List<String> tags;
    private String category;
    private String notes;
    private boolean archived;
    private Instant createdAt;
    private Instant updatedAt;
}
