package com.example.authservice.mail.service;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildVerificationBody(String token) {
        return "Please verify your email using token: " + token;
    }

    public String buildPasswordResetBody(String token) {
        return "Please reset your password using token: " + token;
    }
}

