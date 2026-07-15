package com.querymind.query.domain;

import com.querymind.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "query_history")
public class QueryHistory extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "sql_text", nullable = false, columnDefinition = "TEXT")
    private String sqlText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueryStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "row_count")
    private Integer rowCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuerySource source;

    protected QueryHistory() {}

    public QueryHistory(UUID workspaceId, UUID connectionId, UUID userId, String sqlText,
            QueryStatus status, String rejectionReason, Long durationMs, Integer rowCount,
            QuerySource source) {
        this.workspaceId = workspaceId;
        this.connectionId = connectionId;
        this.userId = userId;
        this.sqlText = sqlText;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.durationMs = durationMs;
        this.rowCount = rowCount;
        this.source = source;
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getConnectionId() { return connectionId; }
    public UUID getUserId() { return userId; }
    public String getSqlText() { return sqlText; }
    public QueryStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public Long getDurationMs() { return durationMs; }
    public Integer getRowCount() { return rowCount; }
    public QuerySource getSource() { return source; }
}
