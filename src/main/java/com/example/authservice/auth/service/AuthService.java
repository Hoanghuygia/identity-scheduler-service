package com.example.authservice.auth.service;

import com.example.authservice.auth.dto.AuthResponse;
import com.example.authservice.auth.dto.ForgotPasswordRequest;
import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.auth.dto.RefreshTokenRequest;
import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.auth.dto.ResetPasswordRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void verifyEmail(String token);

    AuthResponse me();

    void revokeSession(java.util.UUID sessionId);

    void logout(RefreshTokenRequest request);

    void revokeAllSessions();
}
