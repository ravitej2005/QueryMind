package com.querymind.connection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateConnectionRequest(
        @NotBlank String name,
        @NotBlank String host,
        @Min(1) @Max(65535) int port,
        @NotBlank String databaseName,
        @NotBlank String username,
        @NotBlank String password) {}
