package com.example.authservice.common.util;

import com.example.authservice.common.exception.AppException;
import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.security.AuthUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityContextUtilTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldExtractUserIdFromAuthUserPrincipal() {
        UUID userId = UUID.randomUUID();
        AuthUserPrincipal principal = AuthUserPrincipal.builder()
            .userId(userId)
            .email("principal@example.com")
            .password("password")
            .roles(Set.of("ROLE_CUSTOMER"))
            .enabled(true)
            .build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID currentUserId = SecurityContextUtil.currentUserId();

        assertEquals(userId, currentUserId);
    }

    @Test
    void shouldExtractUserIdFromUuidPrincipal() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userId,
            null,
            List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID currentUserId = SecurityContextUtil.currentUserId();

        assertEquals(userId, currentUserId);
    }

    @Test
    void shouldExtractUserIdFromStringPrincipal() {
        UUID userId = UUID.randomUUID();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            userId.toString(),
            null,
            List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID currentUserId = SecurityContextUtil.currentUserId();

        assertEquals(userId, currentUserId);
    }

    @Test
    void shouldThrowUnauthorizedWhenPrincipalIsNotUuidString() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "not-a-uuid",
            null,
            List.of()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AppException exception = assertThrows(AppException.class, SecurityContextUtil::currentUserId);

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void shouldThrowUnauthorizedWhenAuthenticationMissing() {
        AppException exception = assertThrows(AppException.class, SecurityContextUtil::currentUserId);

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void shouldThrowUnauthorizedWhenAuthenticationIsAnonymous() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
            "key",
            "anonymousUser",
            List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AppException exception = assertThrows(AppException.class, SecurityContextUtil::currentUserId);

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }
}
