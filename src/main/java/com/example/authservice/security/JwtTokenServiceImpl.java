package com.example.authservice.security;

import com.example.authservice.config.AppProperties;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final AppProperties appProperties;

    public JwtTokenServiceImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public String generateAccessToken(UUID userId, String email, Set<String> roles) {
        // TODO: Implement RS256 JWT access token generation.
        return "stub-access-token-for-" + userId;
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        // TODO: Implement JWT/opaque refresh token generation strategy.
        return "stub-refresh-token-for-" + userId;
    }

    @Override
    public boolean isTokenValid(String token) {
        // TODO: Implement signature, expiration, issuer, and audience validations.
        return false;
    }

    @Override
    public UUID extractUserId(String token) {
        // TODO: Parse token and extract user subject.
        return null;
    }

    @Override
    public Set<String> extractRoles(String token) {
        // TODO: Parse token and extract roles claim.
        return Collections.emptySet();
    }
}

