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
public class VirusScanResult {
    private VirusScanStatus status;
    private String scannerName;
    private String scannerVersion;
    private Instant scannedAt;
    private String threatName;
    private String threatType;
    private long scanDurationMs;
    private String details;

    public enum VirusScanStatus {
        NOT_SCANNED,
        CLEAN,
        INFECTED,
        SUSPICIOUS,
        QUARANTINED,
        ERROR,
        SKIPPED
    }
}
