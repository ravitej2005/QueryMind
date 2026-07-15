package com.querymind.connection.dto;

import java.time.Instant;
import java.util.UUID;

public record ConnectionResponse(
        UUID id, String name, String dbType, String host, int port,
        String databaseName, String username, Instant createdAt) {}
