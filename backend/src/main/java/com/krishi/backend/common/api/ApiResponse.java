package com.krishi.backend.common.api;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ApiResponse<T>(
        boolean success,
        String message,
        String errorCode,
        T data,
        LocalDateTime timestamp
) {
}