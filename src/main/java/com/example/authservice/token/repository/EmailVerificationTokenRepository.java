package com.example.authservice.token.repository;

import com.example.authservice.token.entity.TokenPurpose;
import com.example.authservice.token.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByToken(String token);
    Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);
    Optional<EmailVerificationToken> findByTokenAndUsedFalseAndPurpose(String token, TokenPurpose purpose);
    List<EmailVerificationToken> findAllByUserIdAndPurposeAndUsedFalse(UUID userId, TokenPurpose purpose);
}

