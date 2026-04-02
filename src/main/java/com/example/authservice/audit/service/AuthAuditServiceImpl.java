package com.example.authservice.audit.service;

import com.example.authservice.audit.entity.AuditEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class AuthAuditServiceImpl implements AuthAuditService {

    @Override
    public void record(UUID userId, AuditEventType eventType, String description, String ipAddress, String userAgent) {
        // TODO: Persist audit records to auth_audit_logs repository.
        log.info("audit_event type={} userId={} ipAddress={} userAgent={} description={}",
            eventType, userId, ipAddress, userAgent, description);
    }
}

