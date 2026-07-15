package com.querymind.workspace.dto;

import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, UUID ownerId, String role) {}
