package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.EmailVerificationRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email")
@Tag(name = "Email Verification", description = "Email verification and resend APIs")
public class EmailVerificationController {

    private final AuthenticationService authService;

    public EmailVerificationController(AuthenticationService authService) {
        this.authService = authService;
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email address using token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully."));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody EmailVerificationRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success(null, "If the email is registered and unverified, a verification link has been sent."));
    }
}
