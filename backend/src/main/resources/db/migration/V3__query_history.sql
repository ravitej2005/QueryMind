CREATE TABLE query_history (
    id BINARY(16) NOT NULL PRIMARY KEY,
    workspace_id BINARY(16) NOT NULL,
    connection_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    sql_text TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,           -- SUCCESS | REJECTED | ERROR
    rejection_reason VARCHAR(500),
    duration_ms BIGINT,
    row_count INT,
    source VARCHAR(20) NOT NULL,           -- MANUAL | AI
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_query_history_workspace ON query_history(workspace_id, created_at);
CREATE INDEX idx_query_history_connection ON query_history(connection_id, created_at);
