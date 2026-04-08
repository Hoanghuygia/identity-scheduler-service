package com.example.authservice.token.service;

import com.example.authservice.token.entity.EmailVerificationToken;
import com.example.authservice.token.repository.EmailVerificationTokenRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenServiceImpl implements VerificationTokenService {

    private static final int TOKEN_VALIDITY_HOURS = 24;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Override
    public String createEmailVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String tokenValue = generateSecureToken();
        Instant expiresAt = Instant.now().plus(TOKEN_VALIDITY_HOURS, ChronoUnit.HOURS);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        token.setUsed(false);

        tokenRepository.save(token);
        return tokenValue;
    }

    @Override
    public boolean validateEmailVerificationToken(String token) {
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(token);
        
        if (tokenOpt.isEmpty()) {
            return false;
        }

        EmailVerificationToken verificationToken = tokenOpt.get();
        return Instant.now().isBefore(verificationToken.getExpiresAt());
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        return tokenRepository.findByTokenAndUsedFalse(token)
            .map(t -> t.getUser().getId())
            .orElse(null);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}