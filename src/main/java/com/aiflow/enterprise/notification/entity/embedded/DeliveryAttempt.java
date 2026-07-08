package com.aiflow.enterprise.notification.entity.embedded;

import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAttempt {
    private int attemptNumber;
    private Instant attemptedAt;
    private DeliveryStatus status;
    private String channel;
    private String errorMessage;
    private long durationMs;
    private String responseCode;
    private String responseBody;
}
