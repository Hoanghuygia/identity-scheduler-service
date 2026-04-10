package com.example.authservice.auth.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.service.ClientInfoService;
import com.example.authservice.config.AppProperties;
import com.example.authservice.security.JwtTokenService;
import com.example.authservice.token.service.RefreshSessionService;
import com.example.authservice.token.service.VerificationTokenService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailVerificationTest {

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private UserService userService;

    @Mock
    private AuthAuditService authAuditService;

    @Mock
    private AuthProviderService localAuthProviderService;

    @Mock
    private RefreshSessionService refreshSessionService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private ClientInfoService clientInfoService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        AppProperties appProperties = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setAccessTokenExpirySeconds(900L);
        appProperties.setJwt(jwt);

        // Create minimal AuthService with required dependencies
        authService = new AuthServiceImpl(
            userService,
            null,
            null,
            null,
            authAuditService,
            null,
            verificationTokenService,
            localAuthProviderService,
            refreshSessionService,
            jwtTokenService,
            clientInfoService,
            appProperties
        );
    }

    @Test
    void shouldVerifyEmailSuccessfully() {
        // Given
        String token = "valid-verification-token";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);

        when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(true);
        when(verificationTokenService.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(user);

        // When
        authService.verifyEmail(token);

        // Then
        verify(userService).activateUser(userId);
        verify(authAuditService).record(eq(userId), eq(AuditEventType.EMAIL_VERIFIED), eq("Email verified successfully"));
    }

    @Test
    void shouldThrowExceptionForInvalidToken() {
        // Given
        String invalidToken = "invalid-token";
        
        when(verificationTokenService.validateEmailVerificationToken(invalidToken)).thenReturn(false);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.verifyEmail(invalidToken);
        });

        assertEquals("Invalid or expired verification token", exception.getMessage());
        verify(userService, never()).activateUser(any());
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyVerified() {
        // Given
        String token = "valid-token";
        UUID userId = UUID.randomUUID();
        
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);

        when(verificationTokenService.validateEmailVerificationToken(token)).thenReturn(true);
        when(verificationTokenService.getUserIdFromToken(token)).thenReturn(userId);
        when(userService.getById(userId)).thenReturn(user);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.verifyEmail(token);
        });

        assertEquals("Email already verified", exception.getMessage());
    }
}
