package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.LoginRequest;
import com.aiflow.enterprise.dto.request.RefreshTokenRequest;
import com.aiflow.enterprise.dto.request.RegisterRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.AuthResponse;
import com.aiflow.enterprise.security.CustomUserDetails;
import com.aiflow.enterprise.service.AuthenticationService;
import com.aiflow.enterprise.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication APIs — register, login, refresh, logout")
public class AuthController {

    private final AuthenticationService authService;
    private final TokenService tokenService;

    public AuthController(AuthenticationService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return new ResponseEntity<>(
                ApiResponse.success(response, "Registration successful. Please check your email to verify your account."),
                HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refreshToken(
                request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke all tokens")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest httpRequest) {
        String refreshToken = httpRequest.getHeader("X-Refresh-Token");
        authService.logout(userDetails.getId(), refreshToken,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate the current access token")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(
            Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Map<String, Object> data = Map.of(
                    "valid", true,
                    "userId", userDetails.getId(),
                    "username", userDetails.getUsername(),
                    "email", userDetails.getEmail(),
                    "roles", userDetails.getAuthorities().stream()
                            .map(Object::toString)
                            .toList()
            );
            return ResponseEntity.ok(ApiResponse.success(data));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Token is invalid or expired", HttpStatus.UNAUTHORIZED));
    }
}
