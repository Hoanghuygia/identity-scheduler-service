package com.example.authservice.security;

import java.util.Set;
import java.util.UUID;

public interface JwtTokenService {

    String generateAccessToken(UUID userId, String email, Set<String> roles);

    String generateRefreshToken(UUID userId);

    boolean isTokenValid(String token);

    JwtTokenValidationResult validateAccessToken(String token);

    UUID extractUserId(String token);

    Set<String> extractRoles(String token);
}
