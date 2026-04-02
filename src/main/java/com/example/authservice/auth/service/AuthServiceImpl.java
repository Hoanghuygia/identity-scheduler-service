package com.example.authservice.auth.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.ForgotPasswordRequest;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.dto.ResetPasswordRequest;
import com.example.authservice.mail.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthAuditService authAuditService;
    private final EmailService emailService;

    @Override
    public AuthResponse register(RegisterRequest request) {
        // TODO: Implement registration flow, user creation, role assignment, and verification token generation.
        authAuditService.record(null, AuditEventType.REGISTER_SUCCESS, "Stub register endpoint called", null, null);
        return AuthResponse.stub("Register stub response");
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

