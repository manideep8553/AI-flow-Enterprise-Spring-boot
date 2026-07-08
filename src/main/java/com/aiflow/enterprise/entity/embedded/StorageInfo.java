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
public class StorageInfo {
    private String storageClass;
    private long originalSize;
    private long compressedSize;
    private String compressionAlgorithm;
    private double compressionRatio;
    private Instant optimizedAt;
    private String optimizationAction;
    private long storageSavingsBytes;
}
