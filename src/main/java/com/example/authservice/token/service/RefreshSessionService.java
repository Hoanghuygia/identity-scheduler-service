package com.example.authservice.token.service;

import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;

import java.util.UUID;

public interface RefreshSessionService {

    RefreshToken createSession(
        UUID userId,
        AuthProviderType provider,
        String providerSubject,
        String deviceInfo,
        String ipAddress
    );

    RefreshToken rotate(String refreshToken, String deviceInfo, String ipAddress);

    void revokeBySessionId(UUID userId, UUID sessionId);

    void revokeCurrent(UUID userId, String refreshToken);

    int revokeAll(UUID userId);

    UUID extractUserIdFromRefreshToken(String refreshToken);
}
