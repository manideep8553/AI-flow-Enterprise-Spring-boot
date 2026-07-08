package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.UserSessionResponse;
import com.aiflow.enterprise.entity.UserSession;
import com.aiflow.enterprise.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/sessions")
@Tag(name = "Session Management", description = "Active session listing and revocation APIs")
public class SessionController {

    private final TokenService tokenService;

    public SessionController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping
    @Operation(summary = "List all active sessions for the current user")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> getActiveSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = ((com.aiflow.enterprise.security.CustomUserDetails) userDetails).getId();
        List<UserSession> sessions = tokenService.getActiveSessions(userId);
        List<UserSessionResponse> response = sessions.stream()
                .map(s -> UserSessionResponse.builder()
                        .id(s.getId())
                        .userId(s.getUserId())
                        .active(s.isActive())
                        .lastActivityAt(s.getLastActivityAt())
                        .expiresAt(s.getExpiresAt())
                        .createdAt(s.getCreatedAt())
                        .ipAddress(s.getIpAddress())
                        .deviceId(s.getDeviceId())
                        .deviceName(s.getDeviceName())
                        .deviceType(s.getDeviceType())
                        .userAgent(s.getUserAgent())
                        .location(s.getLocation())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a specific session")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable String sessionId) {
        tokenService.endSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Session revoked successfully."));
    }

    @DeleteMapping
    @Operation(summary = "Revoke all active sessions")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = ((com.aiflow.enterprise.security.CustomUserDetails) userDetails).getId();
        tokenService.revokeAllUserTokens(userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All sessions revoked successfully."));
    }
}
