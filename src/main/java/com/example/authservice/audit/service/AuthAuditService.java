package com.example.authservice.audit.service;

import com.example.authservice.audit.entity.AuditEventType;

import java.util.UUID;

public interface AuthAuditService {

    void record(UUID userId, AuditEventType eventType, String description);
}

