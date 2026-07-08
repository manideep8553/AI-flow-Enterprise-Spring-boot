package com.aiflow.enterprise.notification.entity;

import com.aiflow.enterprise.entity.BaseEntity;
import com.aiflow.enterprise.notification.entity.embedded.LocalizedContent;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    @Indexed
    private NotificationType type;

    private List<NotificationChannel> supportedChannels;

    private String subjectTemplate;

    private String bodyTemplate;

    private String htmlBodyTemplate;

    private String pushTitleTemplate;

    private String pushBodyTemplate;

    private String smsBodyTemplate;

    private List<LocalizedContent> localizedContents;

    private Map<String, Object> defaultContext;

    private List<String> requiredContextKeys;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int version = 1;
}
