package com.aiflow.enterprise.entity.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersion {
    private int versionNumber;
    private String s3Key;
    private String thumbnailS3Key;
    private String contentHash;
    private long fileSize;
    private String mimeType;
    private String uploadedBy;
    private Instant uploadedAt;
    private String changeNotes;
    private String storageClass;
}
