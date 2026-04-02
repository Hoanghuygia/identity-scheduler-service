package com.example.authservice.token.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TokenServiceImpl implements TokenService {

    @Override
    public String createRefreshToken(UUID userId) {
        // TODO: Implement persistent refresh token generation and storage.
        return "stub-refresh-token-" + userId;
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        // TODO: Implement lookup, expiration check, and revocation check.
        return false;
    }
}

