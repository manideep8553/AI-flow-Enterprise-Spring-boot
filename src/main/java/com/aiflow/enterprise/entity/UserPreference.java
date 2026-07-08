package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "user_preferences")
public class UserPreference extends BaseEntity {

    @Indexed(unique = true)
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
}
