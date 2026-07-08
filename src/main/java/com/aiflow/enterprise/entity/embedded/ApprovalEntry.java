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
public class ApprovalEntry {
    private String stepId;
    private String stepName;
    private String approver;
    private String approverName;
    private String action;
    private String comment;
    private Instant timestamp;
    private Long durationMs;
}
