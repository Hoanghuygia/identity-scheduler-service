package com.example.authservice.integration;

import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import com.example.authservice.token.repository.RefreshTokenRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSessionFlowIntegrationTest {

    private static final String TEST_PASSWORD_HASH = "password-hash";
    private static final String TEST_FULL_NAME = "Session User";
    private static final String TEST_PHONE = "+1234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanData() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE auth_audit_logs");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    @Test
    void shouldRevokeOwnedSession() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User owner = saveUser(ownerId, "owner@example.com");
        saveRefreshToken(sessionId, owner, "owned-refresh-token");

        mockMvc.perform(post("/api/v1/auth/sessions/{sessionId}/revoke", sessionId)
                .with(authenticated(ownerId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        RefreshToken updated = refreshTokenRepository.findById(sessionId).orElseThrow();
        assertThat(updated).extracting("revoked").isEqualTo(true);
        assertThat(updated).extracting("revokedAt").isNotNull();
    }

    @Test
    void shouldRejectRevokingForeignSession() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID foreignSessionId = UUID.randomUUID();
        saveUser(currentUserId, "current@example.com");
        User anotherUser = saveUser(anotherUserId, "another@example.com");
        saveRefreshToken(foreignSessionId, anotherUser, "foreign-refresh-token");

        mockMvc.perform(post("/api/v1/auth/sessions/{sessionId}/revoke", foreignSessionId)
                .with(authenticated(currentUserId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OWNED"));
    }

    @Test
    void shouldLogoutByRevokingCurrentRefreshToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        String refreshTokenValue = "logout-refresh-token";
        User user = saveUser(userId, "logout@example.com");
        saveRefreshToken(tokenId, user, refreshTokenValue);

        mockMvc.perform(post("/api/v1/auth/logout")
                .with(authenticated(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshTokenValue))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        RefreshToken updated = refreshTokenRepository.findById(tokenId).orElseThrow();
        assertThat(updated).extracting("revoked").isEqualTo(true);
    }

    @Test
    void shouldRejectLogoutWhenRefreshTokenBelongsToAnotherUser() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID ownerUserId = UUID.randomUUID();
        UUID ownerTokenId = UUID.randomUUID();
        String ownerRefreshTokenValue = "foreign-logout-refresh-token";

        saveUser(currentUserId, "current-logout@example.com");
        User owner = saveUser(ownerUserId, "owner-logout@example.com");
        saveRefreshToken(ownerTokenId, owner, ownerRefreshTokenValue);

        mockMvc.perform(post("/api/v1/auth/logout")
                .with(authenticated(currentUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshTokenRequest(ownerRefreshTokenValue))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("SESSION_NOT_OWNED"));

        RefreshToken updated = refreshTokenRepository.findById(ownerTokenId).orElseThrow();
        assertThat(updated).extracting("revoked").isEqualTo(false);
    }

    @Test
    void shouldRevokeAllCurrentUserSessionsOnly() throws Exception {
        UUID currentUserId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID currentSessionAId = UUID.randomUUID();
        UUID currentSessionBId = UUID.randomUUID();
        UUID anotherSessionId = UUID.randomUUID();

        User currentUser = saveUser(currentUserId, "all-current@example.com");
        User anotherUser = saveUser(anotherUserId, "all-another@example.com");

        saveRefreshToken(currentSessionAId, currentUser, "current-session-a");
        saveRefreshToken(currentSessionBId, currentUser, "current-session-b");
        saveRefreshToken(anotherSessionId, anotherUser, "another-session");

        mockMvc.perform(post("/api/v1/auth/sessions/revoke-all")
                .with(authenticated(currentUserId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        RefreshToken updatedA = refreshTokenRepository.findById(currentSessionAId).orElseThrow();
        RefreshToken updatedB = refreshTokenRepository.findById(currentSessionBId).orElseThrow();
        RefreshToken untouchedOther = refreshTokenRepository.findById(anotherSessionId).orElseThrow();

        assertThat(updatedA).extracting("revoked").isEqualTo(true);
        assertThat(updatedB).extracting("revoked").isEqualTo(true);
        assertThat(untouchedOther).extracting("revoked").isEqualTo(false);
    }

    @Test
    void shouldReturnCurrentUserProfileForAuthenticatedRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "me-user@example.com";
        saveUser(userId, email);

        mockMvc.perform(get("/api/v1/auth/me")
                .with(authenticated(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Current user profile fetched successfully"))
            .andExpect(jsonPath("$.data.userId").value(userId.toString()))
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.fullName").value(TEST_FULL_NAME))
            .andExpect(jsonPath("$.data.status").value(UserStatus.ACTIVE.name()))
            .andExpect(jsonPath("$.data.emailVerified").value(true))
            .andExpect(jsonPath("$.data.accessToken").doesNotExist())
            .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
            .andExpect(jsonPath("$.data.sessionId").doesNotExist())
            .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturnUnauthorizedWhenMeCalledWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(UUID userId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userId.toString(),
            null,
            List.of()
        );
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
            .authentication(authentication);
    }

    private User saveUser(UUID userId, String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setPasswordHash(TEST_PASSWORD_HASH);
        user.setFullName(TEST_FULL_NAME);
        user.setPhoneNumber(TEST_PHONE);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private RefreshToken saveRefreshToken(UUID tokenId, User user, String tokenValue) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(tokenId);
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setProvider(AuthProviderType.LOCAL);
        refreshToken.setExpiresAt(Instant.now().plusSeconds(600));
        refreshToken.setRevoked(false);
        refreshToken.setLastUsedAt(Instant.now());
        refreshToken.setUpdatedAt(Instant.now());
        return refreshTokenRepository.save(refreshToken);
    }
}
