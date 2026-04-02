package com.example.authservice.mail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String email, String token) {
        // TODO: Integrate JavaMailSender and template rendering.
        log.info("stub_send_verification_email email={} token={}", email, token);
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        // TODO: Integrate JavaMailSender and template rendering.
        log.info("stub_send_password_reset_email email={} token={}", email, token);
    }
}

