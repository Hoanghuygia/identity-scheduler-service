package com.example.authservice.token.service;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.config.AppProperties;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.token.entity.RefreshToken;
import com.example.authservice.token.repository.RefreshTokenRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshSessionServiceImpl implements RefreshSessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final AppProperties appProperties;

    @Override
    public RefreshToken createSession(
        UUID userId,
        AuthProviderType provider,
        String providerSubject,
        String deviceInfo,
        String ipAddress
    ) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND, HttpStatus.NOT_FOUND, "User not found");
        }

        RefreshToken session = buildSession(user, provider, providerSubject, deviceInfo, ipAddress);
        return refreshTokenRepository.save(session);
    }

    @Override
    @Transactional
    public RefreshToken rotate(String refreshToken, String deviceInfo, String ipAddress) {
        Instant now = Instant.now();

        RefreshToken current = requireUsableTokenForRotation(refreshToken, now);

        current.setRevoked(true);
        current.setRevokedAt(now);
        current.setUpdatedAt(now);

        RefreshToken replacement = buildSession(
            current.getUser(),
            current.getProvider(),
            current.getProviderSubject(),
            deviceInfo,
            ipAddress
        );
        replacement = refreshTokenRepository.save(replacement);

        current.setReplacedBy(replacement);
        refreshTokenRepository.save(current);

        return replacement;
    }

    @Override
    @Transactional
    public void revokeBySessionId(UUID userId, UUID sessionId) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
            .orElseThrow(() -> new AppException(
                ErrorCode.SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Refresh session not found"
            ));

        if (!token.getUser().getId().equals(userId)) {
            throw new AppException(
                ErrorCode.SESSION_NOT_OWNED,
                HttpStatus.FORBIDDEN,
                "Refresh session does not belong to the current user"
            );
        }

        if (token.isRevoked()) {
            return;
        }

        Instant now = Instant.now();
        token.setRevoked(true);
        token.setRevokedAt(now);
        token.setUpdatedAt(now);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void revokeCurrent(UUID userId, String refreshToken) {
        Instant now = Instant.now();

        RefreshToken token = requireUsableToken(refreshToken, now);

        if (!token.getUser().getId().equals(userId)) {
            throw new AppException(
                ErrorCode.SESSION_NOT_OWNED,
                HttpStatus.FORBIDDEN,
                "Refresh session does not belong to the current user"
            );
        }

        token.setRevoked(true);
        token.setRevokedAt(now);
        token.setUpdatedAt(now);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public int revokeAll(UUID userId) {
        Instant now = Instant.now();
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUserId(userId, now);

        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
            token.setRevokedAt(now);
            token.setUpdatedAt(now);
        }

        if (!activeTokens.isEmpty()) {
            refreshTokenRepository.saveAll(activeTokens);
        }

        return activeTokens.size();
    }

    @Override
    public UUID extractUserIdFromRefreshToken(String refreshToken) {
        Instant now = Instant.now();

        RefreshToken token = requireUsableToken(refreshToken, now);

        return token.getUser().getId();
    }

    private RefreshToken requireUsableToken(String refreshToken, Instant now) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new AppException(
                ErrorCode.REFRESH_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is invalid"
            ));

        if (token.isRevoked()) {
            throw new AppException(
                ErrorCode.REFRESH_TOKEN_REVOKED,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is revoked"
            );
        }

        if (!token.getExpiresAt().isAfter(now)) {
            throw new AppException(
                ErrorCode.REFRESH_TOKEN_EXPIRED,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is expired"
            );
        }

        return token;
    }

    private RefreshToken requireUsableTokenForRotation(String refreshToken, Instant now) {
        RefreshToken token = refreshTokenRepository.findByTokenForUpdate(refreshToken)
            .orElseThrow(() -> new AppException(
                ErrorCode.REFRESH_TOKEN_INVALID,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is invalid"
            ));

        if (token.isRevoked()) {
            throw new AppException(
                ErrorCode.REFRESH_TOKEN_REVOKED,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is revoked"
            );
        }

        if (!token.getExpiresAt().isAfter(now)) {
            throw new AppException(
                ErrorCode.REFRESH_TOKEN_EXPIRED,
                HttpStatus.UNAUTHORIZED,
                "Refresh token is expired"
            );
        }

        return token;
    }

    private RefreshToken buildSession(
        User user,
        AuthProviderType provider,
        String providerSubject,
        String deviceInfo,
        String ipAddress
    ) {
        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setProvider(provider == null ? AuthProviderType.LOCAL : provider);
        refreshToken.setProviderSubject(providerSubject);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setExpiresAt(now.plusSeconds(appProperties.getJwt().getRefreshTokenExpirySeconds()));
        refreshToken.setRevoked(false);
        refreshToken.setLastUsedAt(now);
        refreshToken.setUpdatedAt(now);
        return refreshToken;
    }
}
