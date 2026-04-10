package com.example.authservice.security;

import com.example.authservice.common.exception.ErrorCode;
import com.example.authservice.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
        throws IOException, ServletException {

        String authErrorCode = (String) request.getAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR);
        JwtTokenValidationError validationError = resolveValidationError(authErrorCode);

        String message = resolveMessage(validationError);
        List<String> details = resolveDetails(validationError, authException);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse errorResponse = ErrorResponse.of(
            message,
            ErrorCode.UNAUTHORIZED.name(),
            details
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private JwtTokenValidationError resolveValidationError(String rawErrorCode) {
        if (rawErrorCode == null || rawErrorCode.isBlank()) {
            return null;
        }

        try {
            return JwtTokenValidationError.valueOf(rawErrorCode);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveMessage(JwtTokenValidationError validationError) {
        if (validationError == null) {
            return "Authentication required";
        }

        return switch (validationError) {
            case TOKEN_MISSING -> "Access token is missing";
            case TOKEN_EXPIRED -> "Access token has expired";
            case TOKEN_UNSUPPORTED -> "Access token type is not supported";
            case TOKEN_MALFORMED, TOKEN_INVALID_SIGNATURE, TOKEN_INVALID_ISSUER, TOKEN_INVALID ->
                "Access token is invalid";
        };
    }

    private List<String> resolveDetails(JwtTokenValidationError validationError, AuthenticationException authException) {
        if (validationError == null) {
            return List.of(authException.getMessage());
        }

        return switch (validationError) {
            case TOKEN_MISSING -> List.of("Provide a Bearer token in the Authorization header");
            case TOKEN_EXPIRED -> List.of("Please refresh token or login again");
            case TOKEN_UNSUPPORTED -> List.of("Use a supported access token format");
            case TOKEN_MALFORMED -> List.of("Token format is malformed");
            case TOKEN_INVALID_SIGNATURE -> List.of("Token signature validation failed");
            case TOKEN_INVALID_ISSUER -> List.of("Token issuer does not match");
            case TOKEN_INVALID -> List.of("Token validation failed");
        };
    }
}
