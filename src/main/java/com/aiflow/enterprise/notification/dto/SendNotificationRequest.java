package com.aiflow.enterprise.notification.dto;

import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
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
public class SendNotificationRequest {

    @NotBlank
    private String recipientId;

    private String recipientEmail;

    private String recipientPhone;

    private String recipientName;

    private String senderId;

    private String senderName;

    private NotificationType type;

    private NotificationPriority priority;

    private List<NotificationChannel> channels;

    private String subject;

    private String body;

    private String htmlBody;

    private String templateId;

    private String templateName;

    private Map<String, Object> contextData;

    private String locale;

    private Instant scheduleAt;

    private String workflowId;

    private String workflowExecutionId;

    private String requestId;

    private String correlationId;

    private Integer maxRetries;

    private Instant expiresAt;
}
