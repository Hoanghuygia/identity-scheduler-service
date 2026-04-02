package com.example.authservice.auth.dto;

import java.util.List;

public record AuthResponse(
    String subject,
    String accessToken,
    String refreshToken,
    List<String> roles,
    String note
) {

    public static AuthResponse stub(String note) {
        return new AuthResponse(
            "stub-user@example.com",
            "stub-access-token",
            "stub-refresh-token",
            List.of("ROLE_CUSTOMER"),
            note
        );
    }
}

