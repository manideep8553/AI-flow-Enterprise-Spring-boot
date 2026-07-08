package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.ChangePasswordRequest;
import com.aiflow.enterprise.dto.request.ForgotPasswordRequest;
import com.aiflow.enterprise.dto.request.ResetPasswordRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password")
@Tag(name = "Password Management", description = "Forgot password, reset password, change password APIs")
public class PasswordController {

    private final AuthenticationService authService;

    public PasswordController(AuthenticationService authService) {
        this.authService = authService;
    }

    @PostMapping("/forgot")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "If the email exists, a password reset link has been sent."));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password using reset token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Password has been reset successfully."));
    }

    @PostMapping("/change")
    @Operation(summary = "Change password for authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        String userId = ((com.aiflow.enterprise.security.CustomUserDetails) userDetails).getId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Password changed successfully. Please log in again."));
    }
}
