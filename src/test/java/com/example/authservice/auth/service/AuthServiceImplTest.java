package com.example.authservice.auth.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.token.service.VerificationTokenService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthServiceImpl(
            userService,
            roleService, 
            passwordEncoder,
            eventPublisher,
            authAuditService,
            emailService,
            verificationTokenService
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
}
