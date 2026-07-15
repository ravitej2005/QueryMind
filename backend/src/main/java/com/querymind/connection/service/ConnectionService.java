package com.querymind.connection.service;

import com.querymind.connection.dto.ConnectionResponse;
import com.querymind.connection.dto.CreateConnectionRequest;
import com.querymind.connection.dto.TestConnectionResponse;
import java.util.List;
import java.util.UUID;

/**
 * Public interface for the connection module (module boundary rule,
 * memory.md §2). The `query` module (SafeExecutionEngine) depends on this
 * interface — never on ConnectionRepository/DatabaseConnection directly.
 */
public interface ConnectionService {

    TestConnectionResponse testConnection(CreateConnectionRequest req);

    ConnectionResponse create(UUID workspaceId, UUID userId, CreateConnectionRequest req);

    List<ConnectionResponse> list(UUID workspaceId);

    void delete(UUID workspaceId, UUID connectionId);

    /**
     * Resolves the live, read-only DataSource for a connection. Used by the
     * `query` module (SafeExecutionEngine) — the only cross-module data this
     * interface exposes is a DataSource handle, never the DatabaseConnection
     * entity itself (module boundary rule, memory.md §2).
     */
    javax.sql.DataSource resolveDataSource(UUID workspaceId, UUID connectionId);

    com.querymind.connection.schema.SchemaModels.SchemaSnapshot getSchema(UUID workspaceId, UUID connectionId);
}
