package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;

public record CurrentUserResponse(
    String userId,
    String email,
    String fullName,
    UserStatus status,
    boolean emailVerified
) {
}
