package com.aiflow.enterprise.notification.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.notification.dto.NotificationResponse;
import com.aiflow.enterprise.notification.dto.NotificationStatsResponse;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import com.aiflow.enterprise.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification sent"));
    }

    @PostMapping("/send-and-publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> sendAndPublish(
            @Valid @RequestBody SendNotificationRequest request) {
        NotificationResponse response = notificationService.sendAndPublish(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification sent and published to queue"));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(
                page, size, userDetails.getUsername(), type, status);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotification(@PathVariable String id) {
        NotificationResponse response = notificationService.getNotificationById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable String id) {
        NotificationResponse response = notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Notification marked as read"));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<NotificationStatsResponse>> getStats() {
        NotificationStatsResponse stats = notificationService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
