package com.aiflow.enterprise.notification.entity;

import com.aiflow.enterprise.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "notification_preferences")
public class NotificationPreference extends BaseEntity {

    @Indexed(unique = true)
    private String userId;

    private boolean emailEnabled;

    private boolean pushEnabled;

    private boolean smsEnabled;

    private boolean slackEnabled;

    private boolean teamsEnabled;

    private boolean inAppEnabled;

    private String emailAddress;

    private String phoneNumber;

    private String slackWebhook;

    private String teamsWebhook;

    private String pushDeviceToken;

    private Map<String, Boolean> typeOverrides;

    private List<String> mutedTypes;

    private boolean quietHoursEnabled;

    private LocalTime quietHoursStart;

    private LocalTime quietHoursEnd;

    private String digestFrequency;

    private List<String> digestTypes;

    private String locale;

    @Builder.Default
    private boolean doNotDisturb = false;

    @Builder.Default
    private int version = 1;
}
