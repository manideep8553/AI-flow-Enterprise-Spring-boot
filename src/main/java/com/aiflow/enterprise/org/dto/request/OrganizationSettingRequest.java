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
public class OrganizationSettingRequest {
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
