package com.querymind.ai.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatHistoryResponse(
        UUID id, String question, String generatedSql, String explanation,
        String status, String rejectionReason, Instant createdAt) {}
