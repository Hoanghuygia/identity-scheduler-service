package com.example.authservice.common.service;

import com.example.authservice.common.dto.UserAgentInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientInfoServiceTest {

    @Mock
    private ServletRequestAttributes requestAttributes;

    private ClientInfoService clientInfoService;

    @BeforeEach
    void setUp() {
        clientInfoService = new ClientInfoServiceImpl();
    }

    @Test
    void getClientIpAddress_withXForwardedFor_returnsFirstIp() {
        // This test will fail until interface and impl are created
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 192.168.1.1");
        
        when(requestAttributes.getRequest()).thenReturn(request);
        
        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::currentRequestAttributes).thenReturn(requestAttributes);
            
            String result = clientInfoService.getClientIpAddress();
            
            assertThat(result).isEqualTo("203.0.113.1");
        }
    }

    @Test
    void getClientIpAddress_withXRealIp_returnsRealIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.2");
        
        when(requestAttributes.getRequest()).thenReturn(request);
        
        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::currentRequestAttributes).thenReturn(requestAttributes);
            
            String result = clientInfoService.getClientIpAddress();
            
            assertThat(result).isEqualTo("203.0.113.2");
        }
    }

    @Test
    void getClientIpAddress_noRequestContext_returnsUnknown() {
        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::currentRequestAttributes)
                .thenThrow(new IllegalStateException("No request context"));
            
            String result = clientInfoService.getClientIpAddress();
            
            assertThat(result).isEqualTo("UNKNOWN");
        }
    }

    @Test
    void getUserAgent_withValidHeader_returnsUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        
        when(requestAttributes.getRequest()).thenReturn(request);
        
        try (MockedStatic<RequestContextHolder> mockedHolder = mockStatic(RequestContextHolder.class)) {
            mockedHolder.when(RequestContextHolder::currentRequestAttributes).thenReturn(requestAttributes);
            
            String result = clientInfoService.getUserAgent();
            
            assertThat(result).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        }
    }

    @Test
    void parseUserAgent_withChromeUserAgent_returnsParsedInfo() {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";
        
        UserAgentInfo result = clientInfoService.parseUserAgent(userAgent);
        
        assertThat(result.browserName()).isEqualTo("Chrome");
        assertThat(result.browserVersion()).isEqualTo("91");
        assertThat(result.operatingSystem()).isEqualTo("Windows 10");
        assertThat(result.deviceType()).isEqualTo("Desktop");
    }

    @Test
    void parseUserAgent_withEmptyUserAgent_returnsUnknown() {
        UserAgentInfo result = clientInfoService.parseUserAgent("");
        
        assertThat(result).isEqualTo(UserAgentInfo.unknown());
    }
}