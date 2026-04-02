package com.example.authservice.auth.validator;

import org.springframework.stereotype.Component;

@Component
public class PasswordStrengthValidator {

    public boolean isStrongEnough(String rawPassword) {
        // TODO: Implement stronger password policy checks.
        return rawPassword != null && rawPassword.length() >= 8;
    }
}

