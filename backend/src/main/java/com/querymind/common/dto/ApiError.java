package com.querymind.common.dto;

public record ApiError(String code, String message, String traceId) {}
