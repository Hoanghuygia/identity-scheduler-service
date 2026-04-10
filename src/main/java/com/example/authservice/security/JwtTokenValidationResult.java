package com.example.authservice.security;

public record JwtTokenValidationResult(boolean valid, JwtTokenValidationError error) {

    public static JwtTokenValidationResult success() {
        return new JwtTokenValidationResult(true, null);
    }

    public static JwtTokenValidationResult failure(JwtTokenValidationError error) {
        return new JwtTokenValidationResult(false, error);
    }
}
