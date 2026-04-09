package com.example.authservice.common.service;

import com.example.authservice.common.dto.UserAgentInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ua_parser.Client;
import ua_parser.Parser;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ClientInfoServiceImpl implements ClientInfoService {
    
    private static final String UNKNOWN_IP = "UNKNOWN";
    private static final String UNKNOWN_USER_AGENT = "N/A";
    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
        "X-Forwarded-For",
        "X-Real-IP", 
        "X-Client-IP",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR"
    );

    @Override
    public String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            for (String header : IP_HEADER_CANDIDATES) {
                String ip = request.getHeader(header);
                if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                    // Handle comma-separated IPs (take first)
                    if (ip.contains(",")) {
                        ip = ip.split(",")[0].trim();
                    }
                    if (isValidIp(ip)) {
                        return ip;
                    }
                }
            }
            
            // Fallback to remote address
            String remoteAddr = request.getRemoteAddr();
            return StringUtils.hasText(remoteAddr) ? remoteAddr : UNKNOWN_IP;
            
        } catch (IllegalStateException e) {
            log.debug("No request context available, returning unknown IP");
            return UNKNOWN_IP;
        }
    }

    @Override
    public String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            String userAgent = request.getHeader("User-Agent");
            return StringUtils.hasText(userAgent) ? userAgent : UNKNOWN_USER_AGENT;
            
        } catch (IllegalStateException e) {
            log.debug("No request context available, returning unknown user agent");
            return UNKNOWN_USER_AGENT;
        }
    }

    @Override
    public UserAgentInfo parseUserAgent(String userAgent) {
        if (!StringUtils.hasText(userAgent) || UNKNOWN_USER_AGENT.equals(userAgent)) {
            return UserAgentInfo.unknown();
        }
        
        try {
            Parser uaParser = new Parser();
            Client client = uaParser.parse(userAgent);
            
            String browserName = client.userAgent.family != null ? client.userAgent.family : "Unknown Browser";
            String browserVersion = client.userAgent.major != null ? client.userAgent.major : "Unknown Version";
            
            // Build OS name with version if available
            String osName = client.os.family != null ? client.os.family : "Unknown OS";
            if (client.os.major != null && !"Unknown OS".equals(osName)) {
                osName = osName + " " + client.os.major;
            }
            
            // Handle device type - convert "Other" to "Desktop"
            String deviceType = "Desktop"; // Default for unknown/Other
            if (client.device.family != null && !"Other".equals(client.device.family)) {
                deviceType = client.device.family;
            }
            
            return new UserAgentInfo(browserName, browserVersion, osName, deviceType);
            
        } catch (Exception e) {
            log.warn("Failed to parse user agent: {}", userAgent, e);
            return UserAgentInfo.unknown();
        }
    }
    
    private boolean isValidIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        
        // Basic IP validation - check for valid IPv4 format
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}