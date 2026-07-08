package com.aiflow.enterprise.org.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceRequest {
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
}
