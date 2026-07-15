package com.querymind.connection.controller;

import com.querymind.connection.dto.ConnectionResponse;
import com.querymind.connection.dto.CreateConnectionRequest;
import com.querymind.connection.dto.TestConnectionResponse;
import com.querymind.connection.schema.SchemaModels.SchemaSnapshot;
import com.querymind.connection.service.ConnectionService;
import com.querymind.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connections")
public class ConnectionController {

    private final ConnectionService connectionService;
    private final WorkspaceService workspaceService;

    public ConnectionController(ConnectionService connectionService, WorkspaceService workspaceService) {
        this.connectionService = connectionService;
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "Test a database connection without saving it")
    @PostMapping("/test")
    public TestConnectionResponse test(
            @PathVariable UUID workspaceId, @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateConnectionRequest req) {
        workspaceService.resolveRole(workspaceId, userId); // ensures membership
        return connectionService.testConnection(req);
    }

    @Operation(summary = "Create a new connection (must pass read-only test first)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionResponse create(
            @PathVariable UUID workspaceId, @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateConnectionRequest req) {
        requireEditorOrOwner(workspaceId, userId);
        return connectionService.create(workspaceId, userId, req);
    }

    @Operation(summary = "List connections in a workspace")
    @GetMapping
    public List<ConnectionResponse> list(@PathVariable UUID workspaceId, @AuthenticationPrincipal UUID userId) {
        workspaceService.resolveRole(workspaceId, userId);
        return connectionService.list(workspaceId);
    }

    @Operation(summary = "Get the introspected schema for a connection")
    @GetMapping("/{connectionId}/schema")
    public SchemaSnapshot schema(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId) {
        workspaceService.resolveRole(workspaceId, userId);
        return connectionService.getSchema(workspaceId, connectionId);
    }

    @Operation(summary = "Delete a connection")
    @DeleteMapping("/{connectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId) {
        requireEditorOrOwner(workspaceId, userId);
        connectionService.delete(workspaceId, connectionId);
    }

    private void requireEditorOrOwner(UUID workspaceId, UUID userId) {
        String role = workspaceService.resolveRole(workspaceId, userId);
        if ("VIEWER".equals(role)) {
            throw new com.querymind.common.exception.ApiException(
                    HttpStatus.FORBIDDEN, "INSUFFICIENT_ROLE", "Viewers cannot modify connections");
        }
    }
}
