package com.example.authservice.security;

public enum JwtTokenValidationError {
    TOKEN_MISSING,
    TOKEN_MALFORMED,
    TOKEN_EXPIRED,
    TOKEN_INVALID_SIGNATURE,
    TOKEN_INVALID_ISSUER,
    TOKEN_UNSUPPORTED,
    TOKEN_INVALID
}
