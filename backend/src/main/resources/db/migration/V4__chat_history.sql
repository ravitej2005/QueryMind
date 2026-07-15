CREATE TABLE chat_messages (
    id BINARY(16) NOT NULL PRIMARY KEY,
    workspace_id BINARY(16) NOT NULL,
    connection_id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    question TEXT NOT NULL,
    generated_sql TEXT,
    explanation TEXT,
    status VARCHAR(20) NOT NULL,   -- ANSWERED | REJECTED | ERROR
    rejection_reason VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_chat_messages_workspace ON chat_messages(workspace_id, created_at);
