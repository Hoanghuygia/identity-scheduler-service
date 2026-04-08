package com.example.authservice.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldAcceptValidRequest() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid-email", "missing@", "@missing.com"})
    void shouldRejectInvalidEmails(String email) {
        RegisterRequest request = new RegisterRequest(
            email,
            "StrongPassword123!",
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"weak", "NoNumbers!", "password123", ""})
    void shouldRejectWeakPasswords(String password) {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            password,
            "John Doe",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectEmptyFullName() {
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "StrongPassword123!",
            "",
            "+1234567890"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}