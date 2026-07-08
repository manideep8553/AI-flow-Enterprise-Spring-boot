package com.aiflow.enterprise.org.controller;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.org.dto.request.UserPreferenceRequest;
import com.aiflow.enterprise.org.dto.response.UserPreferenceResponse;
import com.aiflow.enterprise.org.service.UserPreferenceService;
import com.aiflow.enterprise.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/preferences")
@Tag(name = "User Preferences", description = "User preference management APIs")
public class UserPreferenceController {

    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Get current user's preferences")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> get(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(service.getByUserId(userDetails.getId())));
    }

    @PutMapping
    @Operation(summary = "Update current user's preferences")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> update(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserPreferenceRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(userDetails.getId(), req)));
    }
}
