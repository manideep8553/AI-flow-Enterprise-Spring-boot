package com.aiflow.enterprise.notification.dto;

import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private String id;
    private NotificationType type;
    private NotificationStatus status;
    private NotificationPriority priority;
    private String subject;
    private String body;
    private String htmlBody;
    private String recipientId;
    private String recipientEmail;
    private String recipientPhone;
    private String recipientName;
    private String senderId;
    private String senderName;
    private List<NotificationChannel> channels;
    private String locale;
    private int retryCount;
    private int maxRetries;
    private Instant sentAt;
    private Instant deliveredAt;
    private Instant failedAt;
    private Instant expiresAt;
    private String lastError;
    private List<DeliveryAttempt> deliveryAttempts;
    private String templateId;
    private String workflowId;
    private String workflowExecutionId;
    private String requestId;
    private String correlationId;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
    private Instant updatedAt;
}
