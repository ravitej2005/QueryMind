package com.querymind.ai.service;

import com.querymind.ai.dto.AskRequest;
import com.querymind.ai.dto.AskResponse;
import java.util.UUID;

public interface ChatService {
    AskResponse ask(UUID workspaceId, UUID connectionId, UUID userId, AskRequest request);
}
