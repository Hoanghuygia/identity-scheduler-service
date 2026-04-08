package com.example.authservice.mail.dto;

public record EmailDto(
    String to,
    String subject,
    String url,
    String token
) {
}