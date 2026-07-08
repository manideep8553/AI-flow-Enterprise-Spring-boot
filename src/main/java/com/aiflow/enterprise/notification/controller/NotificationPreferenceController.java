package com.aiflow.enterprise.notification.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.notification.dto.NotificationPreferenceRequest;
import com.aiflow.enterprise.notification.dto.NotificationPreferenceResponse;
import com.aiflow.enterprise.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationService notificationService;

    public NotificationPreferenceController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        NotificationPreferenceResponse response = notificationService.getPreferences(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody NotificationPreferenceRequest request) {
        NotificationPreferenceResponse response = notificationService.updatePreferences(
                userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Preferences updated"));
    }
}
