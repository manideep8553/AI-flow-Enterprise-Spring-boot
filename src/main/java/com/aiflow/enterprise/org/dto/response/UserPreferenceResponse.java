package com.aiflow.enterprise.org.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserPreferenceResponse {
    private String id;
    private String userId;
    private String theme;
    private String language;
    private String timezone;
    private String dateFormat;
    private String timeFormat;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private boolean smsNotifications;
    private boolean weeklyDigest;
    private boolean loginAlerts;
    private Map<String, Object> notificationPreferences;
    private Map<String, Object> dashboardConfig;
    private Map<String, Object> uiPreferences;
    private Instant createdAt;
    private Instant updatedAt;
}
