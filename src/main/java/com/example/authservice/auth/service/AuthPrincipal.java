package com.example.authservice.auth.service;

import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.user.entity.UserStatus;

import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(
    UUID userId,
    String email,
    String fullName,
    UserStatus status,
    Set<String> roles,
    AuthProviderType provider,
    String providerSubject
) {
}
