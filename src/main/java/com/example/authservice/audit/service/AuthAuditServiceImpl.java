package com.example.authservice.audit.service;

import com.example.authservice.audit.entity.AuditEventType;
import com.example.authservice.audit.entity.AuthAuditLog;
import com.example.authservice.audit.repository.AuthAuditLogRepository;
import com.example.authservice.common.dto.UserAgentInfo;
import com.example.authservice.common.service.ClientInfoService;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditServiceImpl implements AuthAuditService {

    private final AuthAuditLogRepository authAuditLogRepository;
    private final UserRepository userRepository;
    private final ClientInfoService clientInfoService;

    @Override
    @Transactional
    public void record(UUID userId, AuditEventType eventType, String description, String ipAddress, String userAgent) {
        try {
            // Extract real client information (ignore passed parameters for backward compatibility)
            String realIpAddress = clientInfoService.getClientIpAddress();
            String realUserAgent = clientInfoService.getUserAgent();
            
            // Parse user agent for additional fields
            UserAgentInfo userAgentInfo = clientInfoService.parseUserAgent(realUserAgent);
            
            // Create audit log entity
            AuthAuditLog auditLog = new AuthAuditLog();
            auditLog.setId(UUID.randomUUID());
            auditLog.setEventType(eventType);
            auditLog.setEventDescription(description);
            auditLog.setIpAddress(realIpAddress);
            auditLog.setUserAgent(realUserAgent);
            auditLog.setBrowserName(userAgentInfo.browserName());
            auditLog.setBrowserVersion(userAgentInfo.browserVersion());
            auditLog.setOperatingSystem(userAgentInfo.operatingSystem());
            auditLog.setDeviceType(userAgentInfo.deviceType());
            
            // Set user if provided and exists
            if (userId != null) {
                userRepository.findById(userId).ifPresent(auditLog::setUser);
            }
            
            // Persist to database
            authAuditLogRepository.save(auditLog);
            
            log.info("audit_event_persisted type={} userId={} ipAddress={} browser={} os={} device={}", 
                eventType, userId, realIpAddress, userAgentInfo.browserName(), 
                userAgentInfo.operatingSystem(), userAgentInfo.deviceType());
                
        } catch (Exception e) {
            // Log error but don't fail primary operation
            log.error("Failed to persist audit record type={} userId={} description={}", 
                eventType, userId, description, e);
        }
    }
}

