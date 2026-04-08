package com.example.authservice.common.service;

import com.example.authservice.common.dto.UserAgentInfo;
import org.springframework.stereotype.Service;

@Service
public class ClientInfoServiceImpl implements ClientInfoService {

    @Override
    public String getClientIpAddress() {
        // TODO: Implement in Task 4
        return "UNKNOWN";
    }

    @Override
    public String getUserAgent() {
        // TODO: Implement in Task 4  
        return "N/A";
    }

    @Override
    public UserAgentInfo parseUserAgent(String userAgent) {
        // TODO: Implement in Task 4
        return null;
    }
}