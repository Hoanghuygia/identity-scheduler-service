package com.example.authservice.common.dto;

/**
 * Parsed user agent information extracted from HTTP request headers.
 * 
 * @param browserName Browser name (e.g., "Chrome", "Firefox")
 * @param browserVersion Browser version (e.g., "91.0.4472.124") 
 * @param operatingSystem Operating system (e.g., "Windows 10", "macOS 12.1")
 * @param deviceType Device type (e.g., "Desktop", "Mobile", "Tablet")
 */
public record UserAgentInfo(
    String browserName,
    String browserVersion,
    String operatingSystem,
    String deviceType
) {
    
    /**
     * Creates UserAgentInfo with unknown values as fallback.
     */
    public static UserAgentInfo unknown() {
        return new UserAgentInfo(
            "Unknown Browser",
            "Unknown Version", 
            "Unknown OS",
            "Unknown Device"
        );
    }
}