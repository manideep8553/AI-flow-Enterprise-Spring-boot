package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.UserPreference;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.dto.request.UserPreferenceRequest;
import com.aiflow.enterprise.org.dto.response.UserPreferenceResponse;
import com.aiflow.enterprise.org.repository.UserPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository repository;

    public UserPreferenceService(UserPreferenceRepository repository) { this.repository = repository; }

    public UserPreferenceResponse getByUserId(String userId) {
        return repository.findByUserId(userId)
                .map(this::toResponse)
                .orElseGet(() -> createDefault(userId));
    }

    public UserPreferenceResponse update(String userId, UserPreferenceRequest req) {
        UserPreference pref = repository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder().userId(userId).build());
        pref.setTheme(req.getTheme()); pref.setLanguage(req.getLanguage());
        pref.setTimezone(req.getTimezone()); pref.setDateFormat(req.getDateFormat());
        pref.setTimeFormat(req.getTimeFormat());
        pref.setEmailNotifications(req.isEmailNotifications());
        pref.setPushNotifications(req.isPushNotifications());
        pref.setSmsNotifications(req.isSmsNotifications());
        pref.setWeeklyDigest(req.isWeeklyDigest());
        pref.setLoginAlerts(req.isLoginAlerts());
        pref.setNotificationPreferences(req.getNotificationPreferences());
        pref.setDashboardConfig(req.getDashboardConfig());
        pref.setUiPreferences(req.getUiPreferences());
        return toResponse(repository.save(pref));
    }

    private UserPreferenceResponse createDefault(String userId) {
        UserPreference pref = UserPreference.builder().userId(userId)
                .theme("light").language("en").timezone("UTC")
                .dateFormat("MM/dd/yyyy").timeFormat("HH:mm")
                .emailNotifications(true).pushNotifications(false)
                .smsNotifications(false).weeklyDigest(true).loginAlerts(true)
                .build();
        return toResponse(repository.save(pref));
    }

    private UserPreferenceResponse toResponse(UserPreference p) {
        return UserPreferenceResponse.builder().id(p.getId()).userId(p.getUserId())
                .theme(p.getTheme()).language(p.getLanguage()).timezone(p.getTimezone())
                .dateFormat(p.getDateFormat()).timeFormat(p.getTimeFormat())
                .emailNotifications(p.isEmailNotifications())
                .pushNotifications(p.isPushNotifications())
                .smsNotifications(p.isSmsNotifications())
                .weeklyDigest(p.isWeeklyDigest()).loginAlerts(p.isLoginAlerts())
                .notificationPreferences(p.getNotificationPreferences())
                .dashboardConfig(p.getDashboardConfig()).uiPreferences(p.getUiPreferences())
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
