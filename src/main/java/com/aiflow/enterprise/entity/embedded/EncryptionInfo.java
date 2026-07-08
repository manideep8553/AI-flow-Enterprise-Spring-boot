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
public class EncryptionInfo {
    private boolean enabled;
    private String algorithm;
    private String kmsKeyId;
    private String envelopeKey;
    private String keyAlgorithm;
    private Instant encryptedAt;
    private String status;
}
