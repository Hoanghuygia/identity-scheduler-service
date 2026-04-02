package com.example.authservice.mail.service;

public interface EmailService {

    void sendVerificationEmail(String email, String token);

    void sendPasswordResetEmail(String email, String token);
}

