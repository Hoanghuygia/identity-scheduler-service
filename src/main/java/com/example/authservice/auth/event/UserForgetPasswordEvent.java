package com.example.authservice.auth.event;

import java.util.UUID;

public record UserForgetPasswordEvent(UUID userId, String email) {
}
