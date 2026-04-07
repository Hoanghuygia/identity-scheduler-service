package com.example.authservice.auth.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.ForgotPasswordRequest;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.dto.ResetPasswordRequest;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthAuditService authAuditService;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("registration_started email={}", request.email());

        // Check if email already exists
        if (userService.findByEmail(request.email()).isPresent()) {
            log.warn("registration_failed_duplicate_email email={}", request.email());
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT, "Email already exists");
        }

        // Create new user in PENDING state
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setPhoneNumber(request.phoneNumber());
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);

        User savedUser = userService.createUser(user);
        
        // Assign CUSTOMER role
        roleService.assignCustomerRole(savedUser.getId());

        // Publish event for async email verification
        UserRegisteredEvent event = new UserRegisteredEvent(savedUser.getId(), savedUser.getEmail());
        eventPublisher.publishEvent(event);

        // Record audit log
        authAuditService.record(
            savedUser.getId(), 
            AuditEventType.REGISTER_SUCCESS, 
            "User registered successfully", 
            null, 
            null
        );

        log.info("registration_completed user_id={} email={}", savedUser.getId(), savedUser.getEmail());

        return new AuthResponse(
            savedUser.getId().toString(),
            savedUser.getEmail(),
            savedUser.getFullName(),
            savedUser.getStatus(),
            null,  // No access token until email verified
            null,  // No refresh token until email verified
            null   // No expiration until email verified
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // TODO: Implement authentication and issue access + refresh tokens.
        authAuditService.record(null, AuditEventType.LOGIN_SUCCESS, "Stub login endpoint called", null, null);
        return AuthResponse.stub("Login stub response");
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        // TODO: Validate refresh token and rotate token pair.
        return AuthResponse.stub("Refresh token stub response");
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // TODO: Generate password reset token and send email.
        emailService.sendPasswordResetEmail(request.email(), "stub-token");
        authAuditService.record(null, AuditEventType.PASSWORD_RESET_REQUESTED, "Stub forgot password called", null, null);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // TODO: Validate reset token and update password hash.
        authAuditService.record(null, AuditEventType.PASSWORD_RESET_COMPLETED, "Stub reset password called", null, null);
    }

    @Override
    public void verifyEmail(String token) {
        // TODO: Validate verification token and mark email as verified.
        authAuditService.record(null, AuditEventType.EMAIL_VERIFIED, "Stub verify email called", null, null);
    }

    @Override
    public AuthResponse me() {
        // TODO: Return authenticated user details from SecurityContext.
        return AuthResponse.stub("Current user stub response");
    }
}

