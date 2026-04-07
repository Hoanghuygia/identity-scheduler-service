package com.example.authservice.token.service;

import java.util.UUID;

public interface VerificationTokenService {
    String createEmailVerificationToken(UUID userId);
    boolean validateEmailVerificationToken(String token);
    UUID getUserIdFromToken(String token);
}