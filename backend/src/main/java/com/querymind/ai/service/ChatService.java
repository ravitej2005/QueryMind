package com.querymind.ai.service;

import com.querymind.ai.dto.AskRequest;
import com.querymind.ai.dto.AskResponse;
import com.querymind.ai.dto.ChatHistoryResponse;
import java.util.List;
import java.util.UUID;

public interface ChatService {
    AskResponse ask(UUID workspaceId, UUID connectionId, UUID userId, AskRequest request);

    /**
     * Paginated chat history for a connection, most recent first. Added to
     * close the gap where every question was persisted but nothing exposed
     * it back to the frontend (Ask workflow "update history" requirement).
     */
    List<ChatHistoryResponse> history(UUID workspaceId, UUID connectionId, int page, int size);
}
