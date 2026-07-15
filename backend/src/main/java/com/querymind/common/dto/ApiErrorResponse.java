package com.querymind.common.dto;

public record ApiErrorResponse(ApiError error) {
    public static ApiErrorResponse of(String code, String message, String traceId) {
        return new ApiErrorResponse(new ApiError(code, message, traceId));
    }
}
