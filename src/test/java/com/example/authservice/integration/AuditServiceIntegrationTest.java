package com.example.authservice.integration;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.entity.AuthAuditLog;
import com.example.authservice.audit.repository.AuthAuditLogRepository;
import com.example.authservice.audit.service.AuthAuditService;
import com.example.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuditServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AuthAuditService authAuditService;

    @Autowired
    private AuthAuditLogRepository authAuditLogRepository;

    @BeforeEach
    @Sql(statements = {
        "SET REFERENTIAL_INTEGRITY FALSE",
        "TRUNCATE TABLE email_verification_tokens RESTART IDENTITY",
        "TRUNCATE TABLE password_reset_tokens RESTART IDENTITY", 
        "TRUNCATE TABLE refresh_tokens RESTART IDENTITY",
        "TRUNCATE TABLE user_roles RESTART IDENTITY",
        "TRUNCATE TABLE auth_audit_logs RESTART IDENTITY",
        "TRUNCATE TABLE users RESTART IDENTITY",
        "SET REFERENTIAL_INTEGRITY TRUE"
    })
    void setUp() {
        // Tables cleaned via @Sql annotation above
    }

    @Test
    void auditService_withHttpRequest_capturesRealClientInfo() {
        // Create register request
        String requestBody = """
            {
                "email": "httptest@example.com",
                "password": "Password123!",
                "fullName": "HTTP Test User",
                "phoneNumber": "+1234567890"
            }
            """;
        
        // Make HTTP request with custom headers to trigger audit logging
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
        
        // Make actual HTTP request to register endpoint to trigger audit logging
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/auth/register",
            HttpMethod.POST,
            request,
            String.class
        );
        
        // Verify HTTP response was successful
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        
        // Verify audit log was persisted with real client info from HTTP headers
        List<AuthAuditLog> auditLogs = authAuditLogRepository.findAll();
        assertThat(auditLogs).hasSize(1);
        
        AuthAuditLog auditLog = auditLogs.get(0);
        assertThat(auditLog.getEventType()).isEqualTo(AuditEventType.REGISTER_SUCCESS);
        assertThat(auditLog.getEventDescription()).isEqualTo("User registered successfully");
        
        // Verify real client info was captured from HTTP headers
        assertThat(auditLog.getIpAddress()).isEqualTo("203.0.113.1"); // First IP from X-Forwarded-For
        assertThat(auditLog.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        assertThat(auditLog.getBrowserName()).isEqualTo("Chrome");
        assertThat(auditLog.getOperatingSystem()).contains("Windows");
        assertThat(auditLog.getDeviceType()).isEqualTo("Desktop");
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