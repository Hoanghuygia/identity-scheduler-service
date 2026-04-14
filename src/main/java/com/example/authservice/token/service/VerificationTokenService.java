package com.example.authservice.token.service;

import com.example.authservice.token.entity.EmailVerificationToken;
import com.example.authservice.token.entity.TokenPurpose;

import java.util.UUID;

public interface VerificationTokenService {
    String createEmailVerificationToken(UUID userId, TokenPurpose purpose);
    boolean validateEmailVerificationToken(String token, TokenPurpose purpose);
    UUID getUserIdFromToken(String token);
    EmailVerificationToken getToken(String token, TokenPurpose purpose);
    void markTokenUsed(EmailVerificationToken token);
    void invalidateUnusedTokens(UUID userId, TokenPurpose purpose);
}
