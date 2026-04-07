package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;

public record AuthResponse(
    String userId,
    String email,
    String fullName,
    UserStatus status,
    String accessToken,
    String refreshToken,
    Long expiresIn
) {
    public static AuthResponse stub(String message) {
        return new AuthResponse(
            "stub-user-id",
            "stub@example.com", 
            "Stub User",
            UserStatus.PENDING,
            null,
            null,
            null
        );
    }

    public static AuthResponse stub(String message, UserStatus status) {
        return new AuthResponse(
            "stub-user-id",
            "stub@example.com",
            "Stub User", 
            status,
            null,
            null,
            null
        );
    }
}

