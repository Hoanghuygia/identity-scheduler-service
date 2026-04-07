package com.example.authservice.auth.dto;

import com.example.authservice.auth.validator.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required") 
    @Email(message = "Email must be valid") 
    String email,
    
    @NotBlank(message = "Password is required")
    @ValidPassword 
    String password,
    
    @NotBlank(message = "Full name is required") 
    @Size(max = 100, message = "Full name must not exceed 100 characters") 
    String fullName,
    
    @Pattern(regexp = "^[+0-9()\\-\\s]{7,20}$", message = "Invalid phone number format") 
    String phoneNumber
) {
}

