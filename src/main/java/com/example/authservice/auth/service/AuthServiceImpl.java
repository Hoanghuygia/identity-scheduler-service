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
import com.example.authservice.common.util.SecurityContextUtil;
import com.example.authservice.config.AppProperties;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.role.service.RoleService;
import com.example.authservice.security.JwtTokenService;
import com.example.authservice.token.entity.RefreshToken;
import com.example.authservice.token.service.RefreshSessionService;
import com.example.authservice.token.service.VerificationTokenService;
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

import java.util.Optional;
import java.util.Set;
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
    private final VerificationTokenService verificationTokenService;
    private final AuthProviderService localAuthProviderService;
    private final RefreshSessionService refreshSessionService;
    private final JwtTokenService jwtTokenService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("registration_started email={}", request.email());

        // Check if email already exists
        Optional<User> existingUserOptional = userService.findByEmail(request.email());
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();
            UserStatus existingStatus = existingUser.getStatus();

            if (existingStatus == UserStatus.SUSPENDED || existingStatus == UserStatus.LOCKED) {
                log.warn("registration_failed_restricted_account email={} status={}", request.email(), existingStatus);
                throw new AppException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "This account is " + existingStatus + ". Please contact admin at admin@cty.com"
                );
            }

            if (existingStatus == UserStatus.PENDING) {
                log.warn("registration_failed_restricted_account email={} status={}", request.email(), existingStatus);
                throw new AppException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN,
                    "This account is " + existingStatus + ". Please activate the account"
                );
            }

            log.warn("registration_failed_duplicate_email email={} status={}", request.email(), existingStatus);
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
            "User registered successfully"
        );

        log.info("registration_completed user_id={} email={}", savedUser.getId(), savedUser.getEmail());

        return new AuthResponse(
            savedUser.getId().toString(),
            savedUser.getEmail(),
            savedUser.getFullName(),
            savedUser.getStatus(),
            null,  // No access token until email verified
            null,  // No refresh token until email verified
            null,  // No expiration until email verified
            null
        );
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            AuthPrincipal principal = localAuthProviderService.authenticate(request);
            RefreshToken refreshSession = refreshSessionService.createSession(
                principal.userId(),
                principal.provider(),
                principal.providerSubject(),
                null,
                null
            );
            String accessToken = jwtTokenService.generateAccessToken(
                principal.userId(),
                principal.email(),
                principal.roles()
            );

            authAuditService.record(principal.userId(), AuditEventType.LOGIN_SUCCESS, "User login successful");

            return new AuthResponse(
                principal.userId().toString(),
                principal.email(),
                principal.fullName(),
                principal.status(),
                accessToken,
                refreshSession.getToken(),
                appProperties.getJwt().getAccessTokenExpirySeconds(),
                refreshSession.getId().toString()
            );
        } catch (AppException ex) {
            authAuditService.record(null, AuditEventType.LOGIN_FAILED, "User login failed: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        try {
            RefreshToken rotatedSession = refreshSessionService.rotate(request.refreshToken(), null, null);
            User user = rotatedSession.getUser();
            Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getCode())
                .collect(java.util.stream.Collectors.toSet());

            String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), roles);

            authAuditService.record(user.getId(), AuditEventType.REFRESH_SUCCESS, "Refresh token rotated successfully");

            return new AuthResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                accessToken,
                rotatedSession.getToken(),
                appProperties.getJwt().getAccessTokenExpirySeconds(),
                rotatedSession.getId().toString()
            );
        } catch (AppException ex) {
            UUID userId = null;
            try {
                userId = refreshSessionService.extractUserIdFromRefreshToken(request.refreshToken());
            } catch (AppException ignored) {
                // No-op, user cannot be resolved for invalid/revoked/expired tokens.
            }

            authAuditService.record(userId, AuditEventType.REFRESH_FAILED, "Refresh token rotation failed: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // TODO: Generate password reset token and send email.
        emailService.sendPasswordResetEmail(request.email(), "stub-token");
        authAuditService.record(null, AuditEventType.PASSWORD_RESET_REQUESTED, "Stub forgot password called");
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // TODO: Validate reset token and update password hash.
        authAuditService.record(null, AuditEventType.PASSWORD_RESET_COMPLETED, "Stub reset password called");
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        log.info("email_verification_started token={}", token);

        // Validate token
        if (!verificationTokenService.validateEmailVerificationToken(token)) {
            log.warn("email_verification_failed_invalid_token token={}", token);
            throw new AppException(
                ErrorCode.INVALID_TOKEN, 
                HttpStatus.BAD_REQUEST, 
                "Invalid or expired verification token"
            );
        }

        // Get user from token
        UUID userId = verificationTokenService.getUserIdFromToken(token);
        User user = userService.getById(userId);
        
        if (user == null) {
            log.warn("email_verification_failed_user_not_found user_id={}", userId);
            throw new AppException(
                ErrorCode.USER_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "User not found"
            );
        }

        // Check if already verified
        if (user.isEmailVerified() || user.getStatus() == UserStatus.ACTIVE) {
            log.warn("email_verification_failed_already_verified user_id={}", userId);
            throw new AppException(
                ErrorCode.EMAIL_ALREADY_VERIFIED,
                HttpStatus.CONFLICT,
                "Email already verified"
            );
        }

        // Activate user
        userService.activateUser(userId);

        // Record audit log
        authAuditService.record(
            userId,
            AuditEventType.EMAIL_VERIFIED,
            "Email verified successfully"
        );  

        log.info("email_verification_completed user_id={}", userId);
    }

    @Override
    public AuthResponse me() {
        // TODO: Return authenticated user details from SecurityContext.
        return AuthResponse.stub("Current user stub response");
    }

    @Override
    public void revokeSession(UUID sessionId) {
        UUID userId = SecurityContextUtil.currentUserId();
        refreshSessionService.revokeBySessionId(userId, sessionId);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        UUID userId = SecurityContextUtil.currentUserId();
        refreshSessionService.revokeCurrent(userId, request.refreshToken());
        authAuditService.record(userId, AuditEventType.LOGOUT, "User logout successful");
    }

    @Override
    public void revokeAllSessions() {
        UUID userId = SecurityContextUtil.currentUserId();
        refreshSessionService.revokeAll(userId);
    }
}
