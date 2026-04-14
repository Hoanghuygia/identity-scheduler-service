package com.example.authservice.auth.service;

import com.example.authservice.auth.event.UserForgetPasswordEvent;
import com.example.authservice.auth.event.UserRegisteredEvent;
import com.example.authservice.mail.service.EmailService;
import com.example.authservice.token.entity.TokenPurpose;
import com.example.authservice.token.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final VerificationTokenService verificationTokenService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        try {
            log.info("handling_user_registered_event user_id={} email={}", 
                event.userId(), event.email());

            String verificationToken = verificationTokenService.createEmailVerificationToken(
                event.userId(),
                TokenPurpose.REGISTER_VERIFICATION
            );
            emailService.sendVerificationEmail(event.email(), verificationToken);

            log.info("verification_email_sent user_id={} email={}", 
                event.userId(), event.email());
        } catch (Exception e) {
            log.error("verification_email_failed user_id={} email={} error={}", 
                event.userId(), event.email(), e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void sentPasswordResetEmail(UserForgetPasswordEvent event) {
        try {
            log.info("handling_user_forget_password_event user_id={} email={}", 
                event.userId(), event.email());

            verificationTokenService.invalidateUnusedTokens(event.userId(), TokenPurpose.PASSWORD_RESET);
            String verificationToken = verificationTokenService.createEmailVerificationToken(
                event.userId(),
                TokenPurpose.PASSWORD_RESET
            );
            emailService.sendPasswordResetEmail(event.email(), verificationToken);

            log.info("password_reset_email_sent user_id={} email={}", 
                event.userId(), event.email());
        } catch (Exception e) {
            log.error("password_reset_email_failed user_id={} email={} error={}", 
                event.userId(), event.email(), e.getMessage(), e);
        }
    }
}
