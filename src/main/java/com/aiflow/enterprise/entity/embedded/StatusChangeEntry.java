package com.aiflow.enterprise.entity.embedded;

import com.aiflow.enterprise.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeEntry {
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private String changedBy;
    private String changedByName;
    private String reason;
    private Instant timestamp;
}
