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
public class OrganizationSettingResponse {
    private String id;
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
    private Instant createdAt;
    private Instant updatedAt;
}
