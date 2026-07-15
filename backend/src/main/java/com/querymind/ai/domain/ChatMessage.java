package com.querymind.ai.domain;

import com.querymind.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "generated_sql", columnDefinition = "TEXT")
    private String generatedSql;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    protected ChatMessage() {}

    public ChatMessage(UUID workspaceId, UUID connectionId, UUID userId, String question,
            String generatedSql, String explanation, ChatStatus status, String rejectionReason) {
        this.workspaceId = workspaceId;
        this.connectionId = connectionId;
        this.userId = userId;
        this.question = question;
        this.generatedSql = generatedSql;
        this.explanation = explanation;
        this.status = status;
        this.rejectionReason = rejectionReason;
    }

    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getConnectionId() { return connectionId; }
    public UUID getUserId() { return userId; }
    public String getQuestion() { return question; }
    public String getGeneratedSql() { return generatedSql; }
    public String getExplanation() { return explanation; }
    public ChatStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
}
