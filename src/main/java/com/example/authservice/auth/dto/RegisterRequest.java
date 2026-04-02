package com.example.authservice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 100) String password,
    @NotBlank @Size(max = 100) String fullName,
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Invalid phone number format") String phoneNumber
) {
}

