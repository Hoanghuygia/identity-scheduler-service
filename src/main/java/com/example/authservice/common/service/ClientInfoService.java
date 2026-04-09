package com.example.authservice.common.service;

import com.example.authservice.common.dto.UserAgentInfo;

/**
 * Service for extracting client information from HTTP requests.
 * Handles proxy headers and user agent parsing.
 */
public interface ClientInfoService {
    
    /**
     * Extracts real client IP address from request headers.
     * Prioritizes X-Forwarded-For, X-Real-IP, X-Client-IP, then remote address.
     * 
     * @return Client IP address, or "UNKNOWN" if no request context available
     */
    String getClientIpAddress();
    
    /**
     * Extracts user agent string from request headers.
     * 
     * @return User agent string, or "N/A" if no request context available
     */
    String getUserAgent();
    
    /**
     * Parses user agent string into structured information.
     * 
     * @param userAgent Raw user agent string
     * @return Parsed user agent information
     */
    UserAgentInfo parseUserAgent(String userAgent);
}