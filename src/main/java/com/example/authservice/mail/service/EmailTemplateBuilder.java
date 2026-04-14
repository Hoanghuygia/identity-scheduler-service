package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {

    public String buildVerificationBody(String token) {
        return "Please verify your email using token: " + token;
    }

    public String buildPasswordResetBody(String token) {
        return "Please reset your password using token: " + token;
    }

    public String buildVerificationEmail(EmailDto emailDto) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Email Verification</title>
            </head>
            <body>
                <h2>Verify Your Email</h2>
                <p>Please click the link below to verify your email address:</p>
                <a href="%s">Verify Email</a>
                <p>If the link doesn't work, copy and paste this URL into your browser:</p>
                <p>%s</p>
            </body>
            </html>
            """, emailDto.url(), emailDto.url());
    }

    public String buildPasswordResetEmail(EmailDto emailDto) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Password Reset</title>
            </head>
            <body>
                <h2>Reset Your Password</h2>
                <p>Reset Token: %s</p>
            </body>
            </html>
            """, emailDto.token());
    }
}

