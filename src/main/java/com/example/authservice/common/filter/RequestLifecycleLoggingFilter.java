package com.example.authservice.common.filter;

import com.example.authservice.common.util.RequestContextUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLifecycleLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();

        log.info("request_start method={} path={} requestId={} traceId={}",
            method, path, RequestContextUtil.requestId(), RequestContextUtil.traceId());

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("request_end method={} path={} status={} durationMs={} requestId={} traceId={}",
                method,
                path,
                response.getStatus(),
                duration,
                RequestContextUtil.requestId(),
                RequestContextUtil.traceId());
        }
    }
}

