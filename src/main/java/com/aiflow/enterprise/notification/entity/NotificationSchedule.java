package com.aiflow.enterprise.notification.entity;

import com.aiflow.enterprise.entity.BaseEntity;
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
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_schedules")
public class NotificationSchedule extends BaseEntity {

    @Indexed
    private String notificationId;

    private String recipientId;

    private String recipientEmail;

    private String recipientPhone;

    private List<NotificationChannel> channels;

    private String subject;

    private String body;

    private String templateId;

    private Map<String, Object> contextData;

    private Instant scheduledAt;

    private Instant processedAt;

    private String recurrence;

    private String cronExpression;

    private String timezone;

    @Builder.Default
    private boolean processed = false;

    private String status;

    @Builder.Default
    private int version = 1;
}
