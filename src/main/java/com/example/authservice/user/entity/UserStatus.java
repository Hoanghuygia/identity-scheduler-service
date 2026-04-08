package com.example.authservice.user.entity;

public enum UserStatus {
    PENDING,     // Newly registered, awaiting email verification
    ACTIVE,      // Verified and can login
    SUSPENDED,   // Admin disabled account
    LOCKED       // Security lockout
}