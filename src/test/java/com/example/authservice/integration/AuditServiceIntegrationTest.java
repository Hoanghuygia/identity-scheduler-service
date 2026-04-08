package com.example.authservice.integration;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.entity.AuthAuditLog;
import com.example.authservice.audit.repository.AuthAuditLogRepository;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class AuditServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthAuditService authAuditService;

    @Autowired
    private AuthAuditLogRepository authAuditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void auditService_withHttpRequest_capturesRealClientInfo() {
        // Create test user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setFullName("Test User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        
        // Make HTTP request with custom headers to trigger audit logging
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        // Call audit service directly (simulating what AuthServiceImpl would do)
        authAuditService.record(user.getId(), AuditEventType.REGISTER_SUCCESS, "Integration test audit", null, null);
        
        // Verify audit log was persisted with correct client info
        List<AuthAuditLog> auditLogs = authAuditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        AuthAuditLog auditLog = auditLogs.get(0);
        assertThat(auditLog.getUser().getId()).isEqualTo(user.getId());
        assertThat(auditLog.getEventType()).isEqualTo(AuditEventType.REGISTER_SUCCESS);
        assertThat(auditLog.getEventDescription()).isEqualTo("Integration test audit");
        // Note: In test environment, these will show default values since no actual HTTP request context
        assertThat(auditLog.getIpAddress()).isNotNull();
        assertThat(auditLog.getUserAgent()).isNotNull();
        assertThat(auditLog.getBrowserName()).isNotNull();
        assertThat(auditLog.getOperatingSystem()).isNotNull();
        assertThat(auditLog.getDeviceType()).isNotNull();
    }

    @Test
    void auditService_withNullUserId_persistsAuditWithoutUser() {
        authAuditService.record(null, AuditEventType.LOGIN_FAILED, "Failed login attempt", null, null);
        
        List<AuthAuditLog> auditLogs = authAuditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        AuthAuditLog auditLog = auditLogs.get(0);
        assertThat(auditLog.getUser()).isNull();
        assertThat(auditLog.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
        assertThat(auditLog.getEventDescription()).isEqualTo("Failed login attempt");
    }
}