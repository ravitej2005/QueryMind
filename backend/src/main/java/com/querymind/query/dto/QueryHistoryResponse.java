package com.querymind.query.dto;

import java.time.Instant;
import java.util.UUID;

public record QueryHistoryResponse(
        UUID id, UUID connectionId, String sqlText, String status,
        String rejectionReason, Long durationMs, Integer rowCount, String source, Instant createdAt) {}
