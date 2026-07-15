package com.querymind.query.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecuteQueryRequest(@NotBlank String sql) {}
