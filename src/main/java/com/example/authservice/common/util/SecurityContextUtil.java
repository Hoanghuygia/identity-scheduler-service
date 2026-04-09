package com.example.authservice.common.util;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.security.AuthUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthUserPrincipal authUserPrincipal) {
            return authUserPrincipal.getUserId();
        }

        if (principal instanceof UUID userId) {
            return userId;
        }

        if (principal instanceof String principalAsString) {
            try {
                return UUID.fromString(principalAsString);
            } catch (IllegalArgumentException ignored) {
                throw new AppException(
                    ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated principal does not contain a valid user ID"
                );
            }
        }

        throw new AppException(
            ErrorCode.UNAUTHORIZED,
            HttpStatus.UNAUTHORIZED,
            "Authenticated principal does not contain a user ID"
        );
    }
}
