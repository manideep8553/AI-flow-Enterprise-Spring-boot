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
public class LifecyclePolicy {
    private int retentionDays;
    private Instant expiresAt;
    private LifecycleAction action;
    private String storageClass;
    private Instant lastReviewedAt;
    private String reviewedBy;
    private boolean notificationSent;

    public enum LifecycleAction {
        DELETE,
        ARCHIVE_TO_GLACIER,
        ARCHIVE_TO_DEEP_ARCHIVE,
        MOVE_TO_STANDARD_IA,
        KEEP
    }
}
