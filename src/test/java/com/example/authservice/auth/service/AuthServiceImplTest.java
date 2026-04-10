package com.example.authservice.auth.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.common.service.ClientInfoService;
import com.example.authservice.common.util.SecurityContextUtil;
import com.example.authservice.config.AppProperties;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.security.JwtTokenService;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import com.example.authservice.token.service.RefreshSessionService;
import com.example.authservice.token.service.VerificationTokenService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthAuditService authAuditService;

    @Mock
    private EmailService emailService;

    @Mock
    private VerificationTokenService verificationTokenService;

    @Mock
    private AuthProviderService localAuthProviderService;

    @Mock
    private RefreshSessionService refreshSessionService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private ClientInfoService clientInfoService;

    private AppProperties appProperties;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        appProperties = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setAccessTokenExpirySeconds(900L);
        appProperties.setJwt(jwt);

        authService = new AuthServiceImpl(
            userService,
            roleService, 
            passwordEncoder,
            eventPublisher,
            authAuditService,
            emailService,
            verificationTokenService,
            localAuthProviderService,
            refreshSessionService,
            jwtTokenService,
            clientInfoService,
            appProperties
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        UUID userId = UUID.randomUUID();
        User savedUser = new User();
        savedUser.setId(userId);
        savedUser.setEmail(request.email());
        savedUser.setFullName(request.fullName());
        savedUser.setStatus(UserStatus.PENDING);

        when(userService.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("hashed-password");
        when(userService.createUser(any(User.class))).thenReturn(savedUser);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertEquals(userId.toString(), response.userId());
        assertEquals(request.email(), response.email());
        assertEquals(request.fullName(), response.fullName());
        assertEquals(UserStatus.PENDING, response.status());
        assertNull(response.accessToken());
        assertNull(response.refreshToken());

        // Verify user creation
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals(request.email(), capturedUser.getEmail());
        assertEquals("hashed-password", capturedUser.getPasswordHash());
        assertEquals(request.fullName(), capturedUser.getFullName());
        assertEquals(request.phoneNumber(), capturedUser.getPhoneNumber());
        assertEquals(UserStatus.PENDING, capturedUser.getStatus());

        // Verify event publication
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        UserRegisteredEvent capturedEvent = eventCaptor.getValue();
        assertEquals(userId, capturedEvent.userId());
        assertEquals(request.email(), capturedEvent.email());

        // Verify role assignment
        verify(roleService).assignCustomerRole(userId);

        // Verify audit logging
        verify(authAuditService).record(eq(userId), any(), eq("User registered successfully"));
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "existing@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        User existingUser = new User();
        existingUser.setStatus(UserStatus.ACTIVE);
        when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.register(request);
        });

        assertEquals("Email already exists", exception.getMessage());
        verify(userService, never()).createUser(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailExistsForSuspendedUser() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "suspended@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        User existingUser = new User();
        existingUser.setStatus(UserStatus.SUSPENDED);
        when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.register(request));

        assertEquals("This account is SUSPENDED. Please contact admin at admin@cty.com", exception.getMessage());
        verify(userService, never()).createUser(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailExistsForLockedUser() {
        // Given
        RegisterRequest request = new RegisterRequest(
            "locked@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        User existingUser = new User();
        existingUser.setStatus(UserStatus.LOCKED);
        when(userService.findByEmail(request.email())).thenReturn(Optional.of(existingUser));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.register(request));

        assertEquals("This account is LOCKED. Please contact admin at admin@cty.com", exception.getMessage());
        verify(userService, never()).createUser(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldLoginWithLocalProviderAndCreateSession() {
        LoginRequest request = new LoginRequest("active@example.com", "StrongPassword123!");
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("CUSTOMER");

        AuthPrincipal principal = new AuthPrincipal(
            userId,
            "active@example.com",
            "Active User",
            UserStatus.ACTIVE,
            roles,
            AuthProviderType.LOCAL,
            "active@example.com"
        );
        when(localAuthProviderService.authenticate(request)).thenReturn(principal);

        RefreshToken createdSession = new RefreshToken();
        createdSession.setId(UUID.randomUUID());
        createdSession.setToken("new-refresh-token");
        when(refreshSessionService.createSession(
            userId,
            AuthProviderType.LOCAL,
            "active@example.com",
            "Mozilla/5.0",
            "203.0.113.10"
        )).thenReturn(createdSession);
        when(clientInfoService.getUserAgent()).thenReturn("Mozilla/5.0");
        when(clientInfoService.getClientIpAddress()).thenReturn("203.0.113.10");

        when(jwtTokenService.generateAccessToken(userId, "active@example.com", roles))
            .thenReturn("new-access-token");

        AuthResponse response = authService.login(request);

        assertEquals(userId.toString(), response.userId());
        assertEquals("active@example.com", response.email());
        assertEquals("Active User", response.fullName());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertEquals("new-access-token", response.accessToken());
        assertEquals("new-refresh-token", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        assertEquals(createdSession.getId().toString(), response.sessionId());
        verify(authAuditService).record(userId, AuditEventType.LOGIN_SUCCESS, "User login successful");
    }

    @Test
    void shouldAuditAndRethrowOnLoginFailure() {
        LoginRequest request = new LoginRequest("unknown@example.com", "invalid");
        AppException exception = new AppException(
            com.example.authservice.common.exception.ErrorCode.INVALID_CREDENTIALS,
            HttpStatus.UNAUTHORIZED,
            "Invalid credentials"
        );
        when(localAuthProviderService.authenticate(request)).thenThrow(exception);

        AppException thrown = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(exception, thrown);
        verify(authAuditService).record(null, AuditEventType.LOGIN_FAILED, "User login failed: Invalid credentials");
    }

    @Test
    void shouldRotateRefreshTokenAndIssueNewAccessToken() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("active@example.com");
        user.setFullName("Active User");
        user.setStatus(UserStatus.ACTIVE);
        user.setUserRoles(Collections.emptySet());

        RefreshToken rotated = new RefreshToken();
        rotated.setId(UUID.randomUUID());
        rotated.setToken("rotated-refresh-token");
        rotated.setUser(user);

        when(refreshSessionService.rotate("old-refresh-token", null, null)).thenReturn(rotated);
        when(jwtTokenService.generateAccessToken(userId, "active@example.com", Collections.emptySet()))
            .thenReturn("rotated-access-token");

        AuthResponse response = authService.refresh(new RefreshTokenRequest("old-refresh-token"));

        assertEquals(userId.toString(), response.userId());
        assertEquals("rotated-access-token", response.accessToken());
        assertEquals("rotated-refresh-token", response.refreshToken());
        assertEquals(900L, response.expiresIn());
        assertEquals(rotated.getId().toString(), response.sessionId());
        verify(authAuditService).record(userId, AuditEventType.REFRESH_SUCCESS, "Refresh token rotated successfully");
    }

    @Test
    void shouldAuditAndRethrowOnRefreshFailure() {
        AppException exception = new AppException(
            com.example.authservice.common.exception.ErrorCode.REFRESH_TOKEN_INVALID,
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid"
        );
        when(refreshSessionService.rotate("bad-token", null, null)).thenThrow(exception);
        when(refreshSessionService.extractUserIdFromRefreshToken("bad-token")).thenThrow(exception);

        AppException thrown = assertThrows(AppException.class,
            () -> authService.refresh(new RefreshTokenRequest("bad-token")));

        assertEquals(exception, thrown);
        verify(authAuditService).record(null, AuditEventType.REFRESH_FAILED, "Refresh token rotation failed: Refresh token is invalid");
    }

    @Test
    void shouldRevokeSessionForCurrentAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

            authService.revokeSession(sessionId);

            verify(refreshSessionService).revokeBySessionId(userId, sessionId);
        }
    }

    @Test
    void shouldReturnCurrentUserProfileFromSecurityContext() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setEmail("active@example.com");
        user.setFullName("Active User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        when(userService.getById(userId)).thenReturn(user);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

            AuthResponse response = authService.me();

            assertEquals(userId.toString(), response.userId());
            assertEquals("active@example.com", response.email());
            assertEquals("Active User", response.fullName());
            assertEquals(UserStatus.ACTIVE, response.status());
            verify(userService).getById(userId);
        }
    }

    @Test
    void shouldThrowNotFoundWhenCurrentUserMissing() {
        UUID userId = UUID.randomUUID();
        when(userService.getById(userId)).thenReturn(null);

        try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

            AppException exception = assertThrows(AppException.class, () -> authService.me());

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("User not found", exception.getMessage());
            verify(userService).getById(userId);
        }
    }

    @Test
    void shouldLogoutByRevokingCurrentRefreshTokenAndRecordingAudit() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-to-revoke");

        try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

            authService.logout(request);

            verify(refreshSessionService).revokeCurrent(userId, "refresh-token-to-revoke");
            verify(authAuditService).record(userId, AuditEventType.LOGOUT, "User logout successful");
        }
    }

    @Test
    void shouldRevokeAllSessionsForCurrentAuthenticatedUser() {
        UUID userId = UUID.randomUUID();

        try (MockedStatic<SecurityContextUtil> mockedSecurityContextUtil = mockStatic(SecurityContextUtil.class)) {
            mockedSecurityContextUtil.when(SecurityContextUtil::currentUserId).thenReturn(userId);

            authService.revokeAllSessions();

            verify(refreshSessionService).revokeAll(userId);
        }
    }
}
