package com.aiflow.enterprise.dto.request;

import com.aiflow.enterprise.entity.embedded.LifecyclePolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String category;
    private List<String> tags;
    private String notes;
    private String requestId;
    private String requestTypeId;
    private boolean generateThumbnail;
    private boolean virusScan;
    private boolean enableEncryption;
    private String kmsKeyId;
    private int retentionDays;
    private LifecyclePolicy.LifecycleAction lifecycleAction;
    private String storageClass;
    private boolean enableCompression;
    private String changeNotes;
}
