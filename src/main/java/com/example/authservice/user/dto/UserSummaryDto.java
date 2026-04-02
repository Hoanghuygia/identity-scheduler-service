package com.example.authservice.user.dto;

import java.util.UUID;

public record UserSummaryDto(
    UUID id,
    String email,
    String fullName,
    String status,
    boolean emailVerified
) {
}

