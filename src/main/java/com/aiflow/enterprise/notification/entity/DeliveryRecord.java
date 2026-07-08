package com.aiflow.enterprise.notification.entity;

import com.aiflow.enterprise.entity.BaseEntity;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "delivery_records")
public class DeliveryRecord extends BaseEntity {

    @Indexed
    private String notificationId;

    @Indexed
    private NotificationChannel channel;

    private String recipientAddress;

    private DeliveryStatus status;

    @Builder.Default
    private int attemptCount = 0;

    @Builder.Default
    private int maxRetries = 3;

    private Instant lastAttemptAt;

    private Instant deliveredAt;

    private Instant failedAt;

    private String lastError;

    private List<DeliveryAttempt> attempts;

    @Builder.Default
    private boolean dlq = false;

    private Instant dlqAt;

    private String dlqReason;

    @Builder.Default
    private int version = 1;
}
