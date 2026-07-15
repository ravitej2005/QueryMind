package com.querymind.connection.service;

import com.querymind.connection.crypto.CredentialCipher;
import com.querymind.connection.datasource.ConnectionDataSourceRegistry;
import com.querymind.connection.domain.DatabaseConnection;
import com.querymind.connection.domain.DatabaseType;
import com.querymind.connection.dto.ConnectionResponse;
import com.querymind.connection.dto.CreateConnectionRequest;
import com.querymind.connection.dto.TestConnectionResponse;
import com.querymind.connection.provider.DatabaseProvider;
import com.querymind.connection.provider.MySqlProvider;
import com.querymind.connection.repository.ConnectionRepository;
import com.querymind.connection.schema.SchemaIntrospectionService;
import com.querymind.connection.schema.SchemaModels.SchemaSnapshot;
import com.querymind.common.exception.ApiException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final CredentialCipher credentialCipher;
    private final MySqlProvider mySqlProvider; // only DatabaseProvider today
    private final ConnectionDataSourceRegistry dataSourceRegistry;
    private final SchemaIntrospectionService schemaIntrospectionService;

    public ConnectionServiceImpl(
            ConnectionRepository connectionRepository,
            CredentialCipher credentialCipher,
            MySqlProvider mySqlProvider,
            ConnectionDataSourceRegistry dataSourceRegistry,
            SchemaIntrospectionService schemaIntrospectionService) {
        this.connectionRepository = connectionRepository;
        this.credentialCipher = credentialCipher;
        this.mySqlProvider = mySqlProvider;
        this.dataSourceRegistry = dataSourceRegistry;
        this.schemaIntrospectionService = schemaIntrospectionService;
    }

    private DatabaseProvider providerFor(DatabaseType type) {
        return switch (type) {
            case MYSQL -> mySqlProvider;
        };
    }

    @Override
    public TestConnectionResponse testConnection(CreateConnectionRequest req) {
        DatabaseConnection transientConn = new DatabaseConnection(
                null, req.name(), DatabaseType.MYSQL, req.host(), req.port(),
                req.databaseName(), req.username(), new byte[0], new byte[0], null);
        try {
            boolean readOnly = mySqlProvider.isReadOnlyCredential(transientConn, req.password());
            if (!readOnly) {
                return new TestConnectionResponse(true, false,
                        "Connected, but this credential has write privileges. Please use a read-only MySQL user.");
            }
            return new TestConnectionResponse(true, true, "Connection verified as read-only.");
        } catch (Exception e) {
            return new TestConnectionResponse(false, false, "Could not connect: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ConnectionResponse create(UUID workspaceId, UUID userId, CreateConnectionRequest req) {
        // Never persist a write-capable credential (phases.md Phase2 checklist).
        TestConnectionResponse test = testConnection(req);
        if (!test.success()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CONNECTION_FAILED", test.message());
        }
        if (!test.readOnly()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CREDENTIAL_NOT_READ_ONLY", test.message());
        }
        var encrypted = credentialCipher.encrypt(req.password());
        DatabaseConnection conn = new DatabaseConnection(
                workspaceId, req.name(), DatabaseType.MYSQL, req.host(), req.port(),
                req.databaseName(), req.username(), encrypted.ciphertext(), encrypted.iv(), userId);
        conn = connectionRepository.save(conn);
        return toResponse(conn);
    }

    @Override
    @Transactional
    public List<ConnectionResponse> list(UUID workspaceId) {
        return connectionRepository.findAllByWorkspaceId(workspaceId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(UUID workspaceId, UUID connectionId) {
        DatabaseConnection conn = requireConnection(workspaceId, connectionId);
        dataSourceRegistry.evict(conn.getId());
        schemaIntrospectionService.invalidate(conn.getId());
        connectionRepository.delete(conn);
    }

    @Override
    @Transactional
    public DataSource resolveDataSource(UUID workspaceId, UUID connectionId) {
        DatabaseConnection conn = requireConnection(workspaceId, connectionId);
        String plaintext = credentialCipher.decrypt(conn.getEncryptedPassword(), conn.getEncryptionIv());
        return dataSourceRegistry.getOrCreate(conn, plaintext, providerFor(conn.getDbType()));
    }

    @Override
    @Transactional
    public SchemaSnapshot getSchema(UUID workspaceId, UUID connectionId) {
        DataSource ds = resolveDataSource(workspaceId, connectionId);
        DatabaseConnection conn = requireConnection(workspaceId, connectionId);
        return schemaIntrospectionService.getSchema(conn.getId(), ds);
    }

    private DatabaseConnection requireConnection(UUID workspaceId, UUID connectionId) {
        return connectionRepository.findByIdAndWorkspaceId(connectionId, workspaceId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "CONNECTION_NOT_FOUND", "Connection not found"));
    }

    private ConnectionResponse toResponse(DatabaseConnection conn) {
        return new ConnectionResponse(
                conn.getId(), conn.getName(), conn.getDbType().name(), conn.getHost(), conn.getPort(),
                conn.getDatabaseName(), conn.getUsername(), conn.getCreatedAt());
    }
}
