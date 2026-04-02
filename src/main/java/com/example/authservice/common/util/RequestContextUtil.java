package com.example.authservice.common.util;

import org.slf4j.MDC;

public final class RequestContextUtil {

    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";

    private RequestContextUtil() {
    }

    public static String requestId() {
        return MDC.get(REQUEST_ID);
    }

    public static String traceId() {
        return MDC.get(TRACE_ID);
    }
}

