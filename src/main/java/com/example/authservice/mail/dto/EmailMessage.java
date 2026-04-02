package com.example.authservice.mail.dto;

public record EmailMessage(
    String to,
    String subject,
    String body
) {
}

