package com.example.authservice.common.filter;

import com.example.authservice.common.util.RequestContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestTracingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String requestId = resolveOrGenerate(request.getHeader(REQUEST_ID_HEADER));
        String traceId = resolveOrGenerate(request.getHeader(TRACE_ID_HEADER));

        MDC.put(RequestContextUtil.REQUEST_ID, requestId);
        MDC.put(RequestContextUtil.TRACE_ID, traceId);

        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestContextUtil.REQUEST_ID);
            MDC.remove(RequestContextUtil.TRACE_ID);
        }
    }

    private String resolveOrGenerate(String value) {
        return StringUtils.hasText(value) ? value : UUID.randomUUID().toString();
    }
}

