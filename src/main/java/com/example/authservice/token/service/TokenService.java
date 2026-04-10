package com.example.authservice.token.service;

import java.util.UUID;

public interface TokenService {

    String createRefreshToken(UUID userId);

    String rotateRefreshToken(String refreshToken);

    boolean isRefreshTokenValid(String token);

    UUID extractUserIdFromRefreshToken(String refreshToken);
}
