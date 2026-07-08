package com.aiflow.enterprise.notification.dto;

import com.aiflow.enterprise.notification.entity.embedded.LocalizedContent;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
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
public class NotificationTemplateResponse {

    private String id;
    private String name;
    private String description;
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
    private boolean enabled;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
