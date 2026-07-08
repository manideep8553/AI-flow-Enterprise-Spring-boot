package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.OrganizationSetting;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.OrganizationSettingRequest;
import com.aiflow.enterprise.org.dto.response.OrganizationSettingResponse;
import com.aiflow.enterprise.org.repository.OrganizationSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrganizationSettingService {

    private final OrganizationSettingRepository repository;

    public OrganizationSettingService(OrganizationSettingRepository repository) {
        this.repository = repository;
    }

    public OrganizationSettingResponse getByOrgId(String orgId) {
        OrganizationSetting s = repository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationSetting", "organizationId", orgId));
        return toResponse(s);
    }

    public OrganizationSettingResponse update(String orgId, OrganizationSettingRequest req) {
        OrganizationSetting s = repository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationSetting", "organizationId", orgId));
        s.setAllowSelfRegistration(req.isAllowSelfRegistration());
        s.setRequireEmailVerification(req.isRequireEmailVerification());
        s.setRequireAdminApprovalForNewUsers(req.isRequireAdminApprovalForNewUsers());
        s.setDefaultTimezone(req.getDefaultTimezone()); s.setDefaultLocale(req.getDefaultLocale());
        s.setDateFormat(req.getDateFormat()); s.setTimeFormat(req.getTimeFormat());
        s.setStartOfWeek(req.getStartOfWeek()); s.setMaxFailedLoginAttempts(req.getMaxFailedLoginAttempts());
        s.setPasswordExpiryDays(req.getPasswordExpiryDays());
        s.setSessionTimeoutMinutes(req.getSessionTimeoutMinutes());
        s.setEnableTwoFactorAuth(req.isEnableTwoFactorAuth());
        s.setEnableAuditLogging(req.isEnableAuditLogging()); s.setLeavePolicy(req.getLeavePolicy());
        s.setWorkDays(req.getWorkDays()); s.setWorkHoursStart(req.getWorkHoursStart());
        s.setWorkHoursEnd(req.getWorkHoursEnd()); s.setHolidayList(req.getHolidayList());
        s.setCustomSettings(req.getCustomSettings());
        return toResponse(repository.save(s));
    }

    private OrganizationSettingResponse toResponse(OrganizationSetting s) {
        return OrganizationSettingResponse.builder().id(s.getId())
                .organizationId(s.getOrganizationId())
                .allowSelfRegistration(s.isAllowSelfRegistration())
                .requireEmailVerification(s.isRequireEmailVerification())
                .requireAdminApprovalForNewUsers(s.isRequireAdminApprovalForNewUsers())
                .defaultTimezone(s.getDefaultTimezone()).defaultLocale(s.getDefaultLocale())
                .dateFormat(s.getDateFormat()).timeFormat(s.getTimeFormat())
                .startOfWeek(s.getStartOfWeek())
                .maxFailedLoginAttempts(s.getMaxFailedLoginAttempts())
                .passwordExpiryDays(s.getPasswordExpiryDays())
                .sessionTimeoutMinutes(s.getSessionTimeoutMinutes())
                .enableTwoFactorAuth(s.isEnableTwoFactorAuth())
                .enableAuditLogging(s.isEnableAuditLogging())
                .leavePolicy(s.getLeavePolicy()).workDays(s.getWorkDays())
                .workHoursStart(s.getWorkHoursStart()).workHoursEnd(s.getWorkHoursEnd())
                .holidayList(s.getHolidayList()).customSettings(s.getCustomSettings())
                .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt()).build();
    }
}
