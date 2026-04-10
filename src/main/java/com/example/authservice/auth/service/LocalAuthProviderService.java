package com.example.authservice.auth.service;

import com.example.authservice.auth.dto.LoginRequest;
import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.token.entity.AuthProviderType;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalAuthProviderService implements AuthProviderService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AuthPrincipal authenticate(LoginRequest request) {
        User user = userService.findByEmail(request.email())
            .orElseThrow(() -> invalidCredentials(request.email()));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidCredentials(request.email());
        }

        if (user.getStatus() != UserStatus.ACTIVE || !user.isEmailVerified()) {
            throw new AppException(
                ErrorCode.FORBIDDEN,
                HttpStatus.FORBIDDEN,
                "Account is not active"
            );
        }

        Set<String> roles = user.getUserRoles().stream()
            .map(userRole -> userRole.getRole().getCode())
            .collect(java.util.stream.Collectors.toSet());

        return new AuthPrincipal(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getStatus(),
            roles,
            AuthProviderType.LOCAL,
            user.getEmail()
        );
    }

    private AppException invalidCredentials(String email) {
        log.warn("local_login_invalid_credentials email={}", email);
        return new AppException(
            ErrorCode.INVALID_CREDENTIALS,
            HttpStatus.UNAUTHORIZED,
            "Invalid credentials"
        );
    }
}
