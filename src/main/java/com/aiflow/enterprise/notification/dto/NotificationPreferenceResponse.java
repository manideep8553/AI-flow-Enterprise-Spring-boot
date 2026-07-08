package com.aiflow.enterprise.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationPreferenceResponse {

    private String id;
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
    private boolean doNotDisturb;
    private Instant createdAt;
    private Instant updatedAt;
}
