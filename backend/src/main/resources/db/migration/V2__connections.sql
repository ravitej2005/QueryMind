CREATE TABLE connections (
    id BINARY(16) NOT NULL PRIMARY KEY,
    workspace_id BINARY(16) NOT NULL,
    name VARCHAR(255) NOT NULL,
    db_type VARCHAR(30) NOT NULL DEFAULT 'MYSQL',
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    database_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    encrypted_password VARBINARY(1024) NOT NULL,
    encryption_iv VARBINARY(64) NOT NULL,
    created_by BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_connection_name UNIQUE (workspace_id, name)
);

CREATE INDEX idx_connections_workspace ON connections(workspace_id);
