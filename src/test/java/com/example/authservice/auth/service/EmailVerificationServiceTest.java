package com.example.authservice.auth.service;

import com.example.authservice.auth.event.UserForgetPasswordEvent;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.token.entity.TokenPurpose;
import com.example.authservice.token.service.VerificationTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailVerificationServiceTest {

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private EmailService emailService;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailVerificationService = new EmailVerificationService(
            verificationTokenService,
            emailService
        );
    }

    @Test
    void shouldHandleUserRegisteredEvent() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = "verification-token-123";
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);

        when(verificationTokenService.createEmailVerificationToken(userId, TokenPurpose.REGISTER_VERIFICATION))
            .thenReturn(token);

        // When
        emailVerificationService.handleUserRegistered(event);

        // Then
        verify(verificationTokenService).createEmailVerificationToken(userId, TokenPurpose.REGISTER_VERIFICATION);
        verify(emailService).sendVerificationEmail(email, token);
    }

    @Test
    void shouldHandleUserForgotPasswordEvent() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = "password-reset-token-123";
        UserForgetPasswordEvent event = new UserForgetPasswordEvent(userId, email);

        when(verificationTokenService.createEmailVerificationToken(userId, TokenPurpose.PASSWORD_RESET))
            .thenReturn(token);

        emailVerificationService.sentPasswordResetEmail(event);

        verify(verificationTokenService).invalidateUnusedTokens(userId, TokenPurpose.PASSWORD_RESET);
        verify(verificationTokenService).createEmailVerificationToken(userId, TokenPurpose.PASSWORD_RESET);
        verify(emailService).sendPasswordResetEmail(email, token);
    }

    @Test
    void shouldHandleEmailServiceException() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = "verification-token-123";
        UserRegisteredEvent event = new UserRegisteredEvent(userId, email);

        when(verificationTokenService.createEmailVerificationToken(userId, TokenPurpose.REGISTER_VERIFICATION))
            .thenReturn(token);
        doThrow(new RuntimeException("Email service failed"))
            .when(emailService).sendVerificationEmail(email, token);

        // When & Then - should not throw exception
        emailVerificationService.handleUserRegistered(event);

        verify(verificationTokenService).createEmailVerificationToken(userId, TokenPurpose.REGISTER_VERIFICATION);
        verify(emailService).sendVerificationEmail(email, token);
    }
}
