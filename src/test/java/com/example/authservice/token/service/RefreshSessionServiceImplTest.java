package com.example.authservice.token.service;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.config.AppProperties;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import com.example.authservice.token.repository.RefreshTokenRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshSessionServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    private RefreshSessionService refreshSessionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        AppProperties appProperties = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setRefreshTokenExpirySeconds(3600L);
        appProperties.setJwt(jwt);

        refreshSessionService = new RefreshSessionServiceImpl(refreshTokenRepository, userService, appProperties);
    }

    @Test
    void shouldCreateSessionWithLastUsedAndUpdatedAt() {
        UUID userId = UUID.randomUUID();
        when(userService.getById(userId)).thenReturn(activeUser(userId));

        RefreshToken saved = new RefreshToken();
        saved.setId(UUID.randomUUID());
        saved.setToken("rt-1");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(saved);

        RefreshToken result = refreshSessionService.createSession(userId, AuthProviderType.LOCAL, null, "agent", "127.0.0.1");

        assertEquals("rt-1", result.getToken());
        verify(refreshTokenRepository).save(argThat(rt ->
            rt.getLastUsedAt() != null && rt.getUpdatedAt() != null && !rt.isRevoked()
        ));
    }

    @Test
    void shouldRotateRefreshTokenAndRevokeOldToken() {
        RefreshToken current = activeToken("old-token", Instant.now().plusSeconds(300));
        when(refreshTokenRepository.findByTokenForUpdate("old-token")).thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = refreshSessionService.rotate("old-token", "agent", "127.0.0.1");

        assertNotEquals("old-token", rotated.getToken());
        assertTrue(current.isRevoked());
        assertNotNull(current.getRevokedAt());
        assertNotNull(current.getUpdatedAt());
        assertNotNull(rotated.getLastUsedAt());
        assertNotNull(rotated.getUpdatedAt());
    }

    @Test
    void shouldRevokeSessionByIdForOwner() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        RefreshToken token = activeToken("x", Instant.now().plusSeconds(300));
        token.setId(sessionId);
        token.getUser().setId(userId);
        when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.of(token));

        refreshSessionService.revokeBySessionId(userId, sessionId);

        assertTrue(token.isRevoked());
        assertNotNull(token.getUpdatedAt());
    }

    @Test
    void shouldThrowSessionNotOwnedWhenRevokingOtherUsersSession() {
        UUID currentUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        RefreshToken token = activeToken("x", Instant.now().plusSeconds(300));
        token.setId(sessionId);
        token.getUser().setId(UUID.randomUUID());
        when(refreshTokenRepository.findById(sessionId)).thenReturn(Optional.of(token));

        AppException exception = assertThrows(AppException.class,
            () -> refreshSessionService.revokeBySessionId(currentUserId, sessionId));

        assertEquals(ErrorCode.SESSION_NOT_OWNED, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void shouldRevokeCurrentTokenForOwner() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = activeToken("current-owner-token", Instant.now().plusSeconds(300));
        token.getUser().setId(userId);
        when(refreshTokenRepository.findByToken("current-owner-token")).thenReturn(Optional.of(token));

        refreshSessionService.revokeCurrent(userId, "current-owner-token");

        assertTrue(token.isRevoked());
        assertNotNull(token.getRevokedAt());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldThrowSessionNotOwnedWhenRevokingCurrentTokenOfAnotherUser() {
        UUID currentUserId = UUID.randomUUID();
        RefreshToken token = activeToken("current-foreign-token", Instant.now().plusSeconds(300));
        token.getUser().setId(UUID.randomUUID());
        when(refreshTokenRepository.findByToken("current-foreign-token")).thenReturn(Optional.of(token));

        AppException exception = assertThrows(AppException.class,
            () -> refreshSessionService.revokeCurrent(currentUserId, "current-foreign-token"));

        assertEquals(ErrorCode.SESSION_NOT_OWNED, exception.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void shouldRevokeAllSessionsForUser() {
        UUID userId = UUID.randomUUID();
        List<RefreshToken> tokens = List.of(
            activeToken("a", Instant.now().plusSeconds(300)),
            activeToken("b", Instant.now().plusSeconds(600))
        );
        when(refreshTokenRepository.findAllActiveByUserId(eq(userId), any(Instant.class))).thenReturn(tokens);

        int revoked = refreshSessionService.revokeAll(userId);

        assertEquals(2, revoked);
        assertTrue(tokens.stream().allMatch(RefreshToken::isRevoked));
    }

    @Test
    void shouldThrowRefreshTokenInvalidWhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
            () -> refreshSessionService.extractUserIdFromRefreshToken("missing"));

        assertEquals(ErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void shouldThrowRefreshTokenExpiredWhenTokenIsExpired() {
        RefreshToken expired = activeToken("expired", Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));

        AppException exception = assertThrows(AppException.class,
            () -> refreshSessionService.extractUserIdFromRefreshToken("expired"));

        assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void shouldThrowRefreshTokenRevokedWhenTokenIsRevoked() {
        RefreshToken revoked = activeToken("revoked", Instant.now().plusSeconds(60));
        revoked.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(revoked));

        AppException exception = assertThrows(AppException.class,
            () -> refreshSessionService.extractUserIdFromRefreshToken("revoked"));

        assertEquals(ErrorCode.REFRESH_TOKEN_REVOKED, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    private User activeUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("active@example.com");
        user.setFullName("Active User");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private RefreshToken activeToken(String tokenValue, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setId(UUID.randomUUID());
        token.setToken(tokenValue);
        token.setExpiresAt(expiresAt);
        token.setRevoked(false);
        token.setUser(activeUser(UUID.randomUUID()));
        return token;
    }
}
