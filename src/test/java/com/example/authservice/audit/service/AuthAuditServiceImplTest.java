package com.example.authservice.audit.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.entity.AuthAuditLog;
import com.example.authservice.audit.repository.AuthAuditLogRepository;
import com.example.authservice.common.dto.UserAgentInfo;
import com.example.authservice.common.service.ClientInfoService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAuditServiceImplTest {

    @Mock
    private AuthAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientInfoService clientInfoService;

    private AuthAuditServiceImpl authAuditService;

    @BeforeEach
    void setUp() {
        authAuditService = new AuthAuditServiceImpl(auditLogRepository, userRepository, clientInfoService);
    }

    @Test
    void record_withValidUserId_persistsAuditLogWithClientInfo() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clientInfoService.getClientIpAddress()).thenReturn("203.0.113.1");
        when(clientInfoService.getUserAgent()).thenReturn("Mozilla/5.0 Chrome/91.0");
        when(clientInfoService.parseUserAgent("Mozilla/5.0 Chrome/91.0"))
            .thenReturn(new UserAgentInfo("Chrome", "91", "Windows 10", "Desktop"));
        
        authAuditService.record(userId, AuditEventType.REGISTER_SUCCESS, "User registered");
        
        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        
        AuthAuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getId()).isNotNull();
        assertThat(savedLog.getUser()).isEqualTo(user);
        assertThat(savedLog.getEventType()).isEqualTo(AuditEventType.REGISTER_SUCCESS);
        assertThat(savedLog.getEventDescription()).isEqualTo("User registered");
        assertThat(savedLog.getIpAddress()).isEqualTo("203.0.113.1");
        assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0 Chrome/91.0");
        assertThat(savedLog.getBrowserName()).isEqualTo("Chrome");
        assertThat(savedLog.getBrowserVersion()).isEqualTo("91");
        assertThat(savedLog.getOperatingSystem()).isEqualTo("Windows 10");
        assertThat(savedLog.getDeviceType()).isEqualTo("Desktop");
    }

    @Test
    void record_withNullUserId_persistsAuditLogWithoutUser() {
        when(clientInfoService.getClientIpAddress()).thenReturn("203.0.113.1");
        when(clientInfoService.getUserAgent()).thenReturn("Mozilla/5.0 Chrome/91.0");
        when(clientInfoService.parseUserAgent("Mozilla/5.0 Chrome/91.0"))
            .thenReturn(new UserAgentInfo("Chrome", "91", "Windows 10", "Desktop"));
        
        authAuditService.record(null, AuditEventType.LOGIN_FAILED, "Invalid credentials");
        
        ArgumentCaptor<AuthAuditLog> logCaptor = ArgumentCaptor.forClass(AuthAuditLog.class);
        verify(auditLogRepository).save(logCaptor.capture());
        
        AuthAuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getUser()).isNull();
        assertThat(savedLog.getEventType()).isEqualTo(AuditEventType.LOGIN_FAILED);
    }
}