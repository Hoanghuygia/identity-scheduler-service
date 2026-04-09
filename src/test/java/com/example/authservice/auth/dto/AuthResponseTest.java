package com.example.authservice.auth.dto;

import com.example.authservice.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void shouldCreateAuthResponseWithStatus() {
        AuthResponse response = new AuthResponse(
            "user123",
            "test@example.com",
            "John Doe",
            UserStatus.PENDING,
            "access-token",
            "refresh-token",
            3600L,
            "session-123"
        );

        assertEquals("user123", response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("John Doe", response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertEquals("access-token", response.accessToken());
        assertEquals("refresh-token", response.refreshToken());
        assertEquals(3600L, response.expiresIn());
        assertEquals("session-123", response.sessionId());
    }

    @Test
    void shouldCreateStubResponseWithoutTokens() {
        AuthResponse response = AuthResponse.stub("Registration successful", UserStatus.PENDING);

        assertEquals("stub-user-id", response.userId());
        assertEquals("stub@example.com", response.email());
        assertEquals("Stub User", response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertNull(response.accessToken());
        assertNull(response.refreshToken());
        assertNull(response.expiresIn());
        assertNull(response.sessionId());
    }
}
