package com.example.authservice.token.service;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceImplTest {

    @Mock
    private RefreshSessionService refreshSessionService;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tokenService = new TokenServiceImpl(refreshSessionService);
    }

    @Test
    void shouldCreateRefreshTokenUsingLocalSessionProvider() {
        UUID userId = UUID.randomUUID();

        RefreshToken session = new RefreshToken();
        session.setToken("created-refresh-token");
        when(refreshSessionService.createSession(userId, AuthProviderType.LOCAL, null, null, null)).thenReturn(session);

        String token = tokenService.createRefreshToken(userId);

        assertEquals("created-refresh-token", token);
    }

    @Test
    void shouldRotateRefreshTokenUsingRefreshSessionService() {
        RefreshToken session = new RefreshToken();
        session.setToken("rotated-refresh-token");
        when(refreshSessionService.rotate("old-refresh-token", null, null)).thenReturn(session);

        String token = tokenService.rotateRefreshToken("old-refresh-token");

        assertEquals("rotated-refresh-token", token);
    }

    @Test
    void shouldReturnTrueWhenRefreshTokenIsValid() {
        when(refreshSessionService.extractUserIdFromRefreshToken("valid-token")).thenReturn(UUID.randomUUID());

        boolean valid = tokenService.isRefreshTokenValid("valid-token");

        assertTrue(valid);
    }

    @Test
    void shouldReturnFalseWhenRefreshTokenIsInvalid() {
        when(refreshSessionService.extractUserIdFromRefreshToken("invalid-token")).thenThrow(
            new AppException(ErrorCode.REFRESH_TOKEN_INVALID, HttpStatus.UNAUTHORIZED, "Refresh token is invalid")
        );

        boolean valid = tokenService.isRefreshTokenValid("invalid-token");

        assertFalse(valid);
    }

    @Test
    void shouldPropagateUnexpectedRuntimeFailureWhenValidatingRefreshToken() {
        when(refreshSessionService.extractUserIdFromRefreshToken("boom-token"))
            .thenThrow(new IllegalStateException("Unexpected failure"));

        assertThrows(IllegalStateException.class, () -> tokenService.isRefreshTokenValid("boom-token"));
    }

    @Test
    void shouldExtractUserIdUsingRefreshSessionService() {
        UUID userId = UUID.randomUUID();
        when(refreshSessionService.extractUserIdFromRefreshToken("valid-token")).thenReturn(userId);

        UUID extractedUserId = tokenService.extractUserIdFromRefreshToken("valid-token");

        assertEquals(userId, extractedUserId);
        verify(refreshSessionService).extractUserIdFromRefreshToken("valid-token");
    }
}
