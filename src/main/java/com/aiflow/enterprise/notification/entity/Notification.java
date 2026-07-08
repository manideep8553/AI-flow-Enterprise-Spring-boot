package com.aiflow.enterprise.notification.entity;

import com.aiflow.enterprise.entity.BaseEntity;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notifications")
@CompoundIndex(name = "notification_lookup", def = "{'type':1, 'status':1, 'createdAt':-1}")
public class Notification extends BaseEntity {

    @Indexed
    private NotificationType type;

    @Indexed
    private NotificationStatus status;

    private NotificationPriority priority;

    private String subject;

    private String body;

    private String htmlBody;

    @Indexed
    private String recipientId;

    private String recipientEmail;

    private String recipientPhone;

    private String recipientName;

    private String senderId;

    private String senderName;

    private List<NotificationChannel> channels;

    private String locale;

    @Builder.Default
    private int retryCount = 0;

    @Builder.Default
    private int maxRetries = 3;

    private Instant nextRetryAt;

    private Instant sentAt;

    private Instant deliveredAt;

    private Instant failedAt;

    private Instant expiresAt;

    private String lastError;

    private List<DeliveryAttempt> deliveryAttempts;

    private Map<String, Object> contextData;

    private String templateId;

    private String workflowId;

    private String workflowExecutionId;

    private String requestId;

    @Indexed
    private String correlationId;

    private boolean read;

    private Instant readAt;

    @Builder.Default
    private int version = 1;
}
