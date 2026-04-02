package com.example.authservice.common.response;

import com.example.authservice.common.util.RequestContextUtil;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final String requestId;
    private final String traceId;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .requestId(RequestContextUtil.requestId())
            .traceId(RequestContextUtil.traceId())
            .build();
    }

    public static ApiResponse<Void> success(String message) {
        return success(message, null);
    }
}

