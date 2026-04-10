package com.example.authservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void shouldReturnMissingTokenMessageWhenTokenIsMissing() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR, JwtTokenValidationError.TOKEN_MISSING.name());
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
            request,
            response,
            new InsufficientAuthenticationException("Full authentication is required to access this resource")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("Access token is missing", body.get("message").asText());
    }

    @Test
    void shouldReturnExpiredTokenMessageWhenTokenIsExpired() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthErrorAttributes.AUTH_ERROR_CODE_ATTR, JwtTokenValidationError.TOKEN_EXPIRED.name());
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
            request,
            response,
            new InsufficientAuthenticationException("Full authentication is required to access this resource")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertEquals("Access token has expired", body.get("message").asText());
    }
}
