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
public class FileAttachment {
    private String id;
    private String fileName;
    private String originalName;
    private String mimeType;
    private long fileSize;
    private String s3Key;
    private String s3Bucket;
    private String url;
    private String uploadedBy;
    private Instant uploadedAt;
    private String category;
}
