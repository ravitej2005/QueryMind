package com.querymind.connection.dto;

public record TestConnectionResponse(boolean success, boolean readOnly, String message) {}
