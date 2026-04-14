package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder templateBuilder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Value("${app.backend.base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Override
    public void sendVerificationEmail(String email, String token) {
        String verificationUrl = String.format("%s/api/v1/auth-test/verify-email-test?token=%s", backendBaseUrl, token);
        
        EmailDto emailDto = new EmailDto(
            email,
            "Email Verification - Identity Service",
            verificationUrl,
            token
        );

        String htmlContent = templateBuilder.buildVerificationEmail(emailDto);
        sendHtmlEmail(email, "Email Verification - Identity Service", htmlContent);
        
        log.info("email_verification_sent recipient={} token_length={}", 
                email, token.length());
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {        
        EmailDto emailDto = new EmailDto(
            email,
            "Password Reset - Identity Service",
            null,
            token
        );

        String htmlContent = templateBuilder.buildPasswordResetEmail(emailDto);
        sendHtmlEmail(email, "Password Reset - Identity Service", htmlContent);
        
        log.info("password_reset_email_sent recipient={} token_length={}", 
                email, token.length());
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            log.info("email_sent to={} subject='{}' from={}", to, subject, fromEmail);
        } catch (MessagingException e) {
            log.error("email_send_failed to={} subject='{}' error={}", to, subject, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}