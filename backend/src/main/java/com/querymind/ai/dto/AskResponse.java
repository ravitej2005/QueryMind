package com.querymind.ai.dto;

import java.util.List;
import java.util.Map;

public record AskResponse(
        boolean success, String generatedSql, List<String> columns, List<Map<String, Object>> rows,
        int rowCount, String explanation, String rejectionReason, String provider) {}
