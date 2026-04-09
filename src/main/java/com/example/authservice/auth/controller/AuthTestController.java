package com.example.authservice.auth.controller;

import com.example.authservice.auth.service.AuthService;
import com.example.authservice.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth-test")
@RequiredArgsConstructor
public class AuthTestController {

    private final AuthService authService;

    @GetMapping("/verify-email-test")
    public ResponseEntity<ApiResponse<String>> verifyEmailTest(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", "ok"));
    }

    @GetMapping("/health-test")
    public ResponseEntity<ApiResponse<String>> healthTest() {
        return ResponseEntity.ok(ApiResponse.success("Protected endpoint is reachable", "ok"));
    }

    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminTest() {
        return ResponseEntity.ok(ApiResponse.success("Role-protected endpoint is reachable", "ok"));
    }
}
