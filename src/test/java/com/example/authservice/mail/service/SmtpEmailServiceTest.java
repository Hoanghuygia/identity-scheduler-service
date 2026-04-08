package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateBuilder templateBuilder;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new SmtpEmailService(mailSender, templateBuilder);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    void shouldSendVerificationEmail() {
        // Given
        String email = "test@example.com";
        String token = "verification-token";
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateBuilder.buildVerificationEmail(any(EmailDto.class)))
            .thenReturn("HTML content");

        // When
        emailService.sendVerificationEmail(email, token);

        // Then
        verify(mailSender).send(mimeMessage);
        verify(templateBuilder).buildVerificationEmail(any(EmailDto.class));
    }

    @Test
    void shouldSendPasswordResetEmail() {
        // Given
        String email = "test@example.com";
        String token = "reset-token";
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateBuilder.buildPasswordResetEmail(any(EmailDto.class)))
            .thenReturn("HTML content");

        // When
        emailService.sendPasswordResetEmail(email, token);

        // Then
        verify(mailSender).send(mimeMessage);
        verify(templateBuilder).buildPasswordResetEmail(any(EmailDto.class));
    }
}