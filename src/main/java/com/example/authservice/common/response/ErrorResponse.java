package com.example.authservice.common.response;

import com.example.authservice.common.util.RequestContextUtil;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success;
    private final String message;
    private final String errorCode;
    private final List<String> details;
    private final String requestId;
    private final String traceId;
    private final Instant timestamp;

    public static ErrorResponse of(String message, String errorCode, List<String> details) {
        return ErrorResponse.builder()
            .success(false)
            .message(message)
            .errorCode(errorCode)
            .details(details)
            .requestId(RequestContextUtil.requestId())
            .traceId(RequestContextUtil.traceId())
            .timestamp(Instant.now())
            .build();
    }
}

