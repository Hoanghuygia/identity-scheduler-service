package com.example.authservice.token.service;

import java.util.UUID;

public interface TokenService {

    String createRefreshToken(UUID userId);

    boolean isRefreshTokenValid(String token);
}

