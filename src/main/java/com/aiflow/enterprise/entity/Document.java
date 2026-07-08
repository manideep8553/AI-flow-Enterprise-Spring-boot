package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.AnomalyResult;
import com.aiflow.enterprise.entity.embedded.DocumentValidationResult;
import com.aiflow.enterprise.entity.embedded.DocumentVersion;
import com.aiflow.enterprise.entity.embedded.DuplicateInfo;
import com.aiflow.enterprise.entity.embedded.EncryptionInfo;
import com.aiflow.enterprise.entity.embedded.ExtractedField;
import com.aiflow.enterprise.entity.embedded.LifecyclePolicy;
import com.aiflow.enterprise.entity.embedded.StorageInfo;
import com.aiflow.enterprise.entity.embedded.VirusScanResult;
import com.aiflow.enterprise.enums.DocumentType;
import com.aiflow.enterprise.enums.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document extends BaseEntity {

    @Indexed
    private String fileName;

    private String originalName;

    private String mimeType;

    private long fileSize;

    @Indexed
    private DocumentType documentType;

    private double documentTypeConfidence;

    @Indexed
    private ProcessingStatus processingStatus;

    @Indexed
    private String uploadedBy;

    private Instant uploadedAt;

    private Instant processedAt;

    private String s3Bucket;

    private String s3Key;

    private String s3Url;

    private String thumbnailS3Key;

    private String previewS3Key;

    private String contentHash;

    private String ocrText;

    private String ocrMethod;

    private List<ExtractedField> extractedFields;

    private Map<String, Object> extractedData;

    private List<DocumentValidationResult> validationResults;

    private List<AnomalyResult> anomalies;

    private DuplicateInfo duplicateInfo;

    private VirusScanResult virusScanResult;

    private EncryptionInfo encryptionInfo;

    private LifecyclePolicy lifecyclePolicy;

    private StorageInfo storageInfo;

    private String summary;

    private String aiAnalysis;

    private String requestId;

    @Indexed
    private String requestTypeId;

    @Builder.Default
    private int pageCount = 1;

    private List<String> tags;

    private String category;

    private double categoryConfidence;

    private String notes;

    @Builder.Default
    private boolean archived = false;

    @Builder.Default
    private int version = 1;

    private List<DocumentVersion> versionHistory;
}
