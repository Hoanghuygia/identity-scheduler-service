package com.example.authservice.auth.controller;

import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.ForgotPasswordRequest;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.dto.ResetPasswordRequest;
import com.example.authservice.auth.service.AuthService;
import com.example.authservice.common.response.ApiResponse;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stub response", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Stub response", authService.refresh(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Stub response"));
    }

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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> me() {
        return ResponseEntity.ok(ApiResponse.success("Stub response", authService.me()));
    }

    @PostMapping("/sessions/{sessionId}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeSession(@PathVariable UUID sessionId) {
        authService.revokeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("Logout successful"));
    }

    @PostMapping("/sessions/revoke-all")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions() {
        authService.revokeAllSessions();
        return ResponseEntity.ok(ApiResponse.success("All sessions revoked successfully"));
    }
}
