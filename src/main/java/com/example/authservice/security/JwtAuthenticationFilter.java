package com.example.authservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(bearerToken)) {
            request.setAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR, JwtTokenValidationError.TOKEN_MISSING.name());
            filterChain.doFilter(request, response);
            return;
        }

        if (!bearerToken.startsWith("Bearer ")) {
            request.setAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR, JwtTokenValidationError.TOKEN_MALFORMED.name());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        String token = bearerToken.substring(7);
        JwtTokenValidationResult validationResult = jwtTokenService.validateAccessToken(token);

        if (validationResult.valid()) {
            UUID userId = jwtTokenService.extractUserId(token);
            log.info("authenticated_request userId={}", userId);

            if (userId != null) {
                var authorities = jwtTokenService.extractRoles(token).stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } else {
            request.setAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR, validationResult.error().name());
            log.warn("invalid_access_token reason={}", validationResult.error());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
