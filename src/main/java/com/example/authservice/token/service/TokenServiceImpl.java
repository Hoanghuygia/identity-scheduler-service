package com.example.authservice.token.service;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RefreshSessionService refreshSessionService;

    @Override
    public String createRefreshToken(UUID userId) {
        RefreshToken session = refreshSessionService.createSession(
            userId,
            AuthProviderType.LOCAL,
            null,
            null,
            null
        );
        return session.getToken();
    }

    @Override
    public String rotateRefreshToken(String refreshToken) {
        RefreshToken rotated = refreshSessionService.rotate(refreshToken, null, null);
        return rotated.getToken();
    }

    @Override
    public boolean isRefreshTokenValid(String token) {
        try {
            refreshSessionService.extractUserIdFromRefreshToken(token);
            return true;
        } catch (AppException ex) {
            ErrorCode errorCode = ex.getErrorCode();
            if (errorCode == ErrorCode.REFRESH_TOKEN_INVALID
                || errorCode == ErrorCode.REFRESH_TOKEN_REVOKED
                || errorCode == ErrorCode.REFRESH_TOKEN_EXPIRED) {
                return false;
            }
            throw ex;
        }
    }

    @Override
    public UUID extractUserIdFromRefreshToken(String refreshToken) {
        return refreshSessionService.extractUserIdFromRefreshToken(refreshToken);
    }
}
