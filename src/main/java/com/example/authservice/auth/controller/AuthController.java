package com.example.authservice.auth.controller;

import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.CurrentUserResponse;
import com.example.authservice.auth.dto.ForgotPasswordRequest;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.dto.ResetPasswordRequest;
import com.example.authservice.auth.service.AuthService;
import com.example.authservice.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // DONE: Implement register endpoint
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Register Successfully", authService.register(request)));
    }

    // DONE: Implement login endpoint
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    // DONE: Implement refresh token endpoint
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Refresh successful", authService.refresh(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Stub response"));
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Stub response"));
    }

    // DONE: Implement email verification endpoint
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Stub response"));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success("Current user profile fetched successfully", authService.me()));
    }

    // DONE: Implement revoke session endpoint
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/sessions/{sessionId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID sessionId) {
        authService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully"));
    }

    // DONE: Implement logout endpoint
    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    // DONE: Implement revoke all sessions endpoint
    @PostMapping("/sessions/revoke-all")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions() {
        authService.revokeAllSessions();
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked successfully"));
    }
}
