package com.querymind.query.dto;

import java.util.List;
import java.util.Map;

public record ExecuteQueryResponse(
        boolean success, List<String> columns, List<Map<String, Object>> rows,
        int rowCount, long durationMs, String rejectionReason) {}
