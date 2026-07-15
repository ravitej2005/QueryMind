package com.querymind.connection.domain;

import com.querymind.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "connections")
public class DatabaseConnection extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "db_type", nullable = false)
    private DatabaseType dbType;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(name = "database_name", nullable = false)
    private String databaseName;

    @Column(nullable = false)
    private String username;

    @Column(name = "encrypted_password", nullable = false)
    private byte[] encryptedPassword;

    @Column(name = "encryption_iv", nullable = false)
    private byte[] encryptionIv;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected DatabaseConnection() {}

    public DatabaseConnection(UUID workspaceId, String name, DatabaseType dbType, String host,
            int port, String databaseName, String username, byte[] encryptedPassword,
            byte[] encryptionIv, UUID createdBy) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.dbType = dbType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.encryptedPassword = encryptedPassword;
        this.encryptionIv = encryptionIv;
        this.createdBy = createdBy;
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public String getName() { return name; }
    public DatabaseType getDbType() { return dbType; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabaseName() { return databaseName; }
    public String getUsername() { return username; }
    public byte[] getEncryptedPassword() { return encryptedPassword; }
    public byte[] getEncryptionIv() { return encryptionIv; }
    public UUID getCreatedBy() { return createdBy; }
}
