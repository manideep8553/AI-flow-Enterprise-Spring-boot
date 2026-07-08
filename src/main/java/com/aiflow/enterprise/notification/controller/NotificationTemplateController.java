package com.aiflow.enterprise.notification.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.notification.dto.NotificationTemplateRequest;
import com.aiflow.enterprise.notification.dto.NotificationTemplateResponse;
import com.aiflow.enterprise.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notification-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class NotificationTemplateController {

    private final NotificationService notificationService;

    public NotificationTemplateController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = notificationService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Template created"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> updateTemplate(
            @PathVariable String id,
            @Valid @RequestBody NotificationTemplateRequest request) {
        NotificationTemplateResponse response = notificationService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Template updated"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationTemplateResponse>> getTemplate(@PathVariable String id) {
        NotificationTemplateResponse response = notificationService.getTemplateById(id);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationTemplateResponse>>> getTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type) {
        Page<NotificationTemplateResponse> templates = notificationService.getTemplates(page, size, type);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String id) {
        notificationService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewTemplate(
            @PathVariable String id,
            @RequestBody Map<String, Object> context,
            @RequestParam(required = false) String locale) {
        Map<String, Object> preview = notificationService.renderTemplatePreview(id, context, locale);
        return ResponseEntity.ok(ApiResponse.success(preview));
    }
}
