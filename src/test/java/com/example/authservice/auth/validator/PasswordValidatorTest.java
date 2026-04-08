package com.example.authservice.auth.validator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Password123!",
        "StrongPass1@",
        "MySecureP4$$w0rd",
        "C0mplex!Password"
    })
    void shouldAcceptValidPasswords(String password) {
        TestRecord record = new TestRecord(password);
        Set<ConstraintViolation<TestRecord>> violations = validator.validate(record);
        assertTrue(violations.isEmpty(), "Password should be valid: " + password);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",           // Too short
        "nouppercase123!",   // No uppercase
        "NOLOWERCASE123!",   // No lowercase  
        "NoNumbers!",        // No numbers
        "NoSpecialChars123", // No special chars
        "Password123",       // No special chars
        ""                   // Empty
    })
    void shouldRejectInvalidPasswords(String password) {
        TestRecord record = new TestRecord(password);
        Set<ConstraintViolation<TestRecord>> violations = validator.validate(record);
        assertFalse(violations.isEmpty(), "Password should be invalid: " + password);
    }

    record TestRecord(@ValidPassword String password) {}
}