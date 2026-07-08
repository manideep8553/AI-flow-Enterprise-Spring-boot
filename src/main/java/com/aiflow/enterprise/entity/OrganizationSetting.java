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
@Document(collection = "organization_settings")
public class OrganizationSetting extends BaseEntity {

    @Indexed(unique = true)
    private String organizationId;

    private boolean allowSelfRegistration;

    private boolean requireEmailVerification;

    private boolean requireAdminApprovalForNewUsers;

    private String defaultTimezone;

    private String defaultLocale;

    private String dateFormat;

    private String timeFormat;

    private String startOfWeek;

    private int maxFailedLoginAttempts;

    private int passwordExpiryDays;

    private int sessionTimeoutMinutes;

    private boolean enableTwoFactorAuth;

    private boolean enableAuditLogging;

    private String leavePolicy;

    private String workDays;

    private String workHoursStart;

    private String workHoursEnd;

    private Map<String, Object> holidayList;

    private Map<String, Object> customSettings;
}
