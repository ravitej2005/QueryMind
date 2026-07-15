package com.querymind.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querymind.ai.cache.AiResponseCache;
import com.querymind.ai.domain.ChatMessage;
import com.querymind.ai.domain.ChatStatus;
import com.querymind.ai.dto.AskRequest;
import com.querymind.ai.dto.AskResponse;
import com.querymind.ai.provider.ResilientAiProvider;
import com.querymind.ai.repository.ChatMessageRepository;
import com.querymind.connection.schema.SchemaModels.SchemaSnapshot;
import com.querymind.connection.service.ConnectionService;
import com.querymind.query.domain.QuerySource;
import com.querymind.query.dto.ExecuteQueryResponse;
import com.querymind.query.service.QueryExecutionService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatServiceImpl implements ChatService {

    private static final String REJECT_PREFIX = "REJECT:";

    private final ResilientAiProvider aiProvider;
    private final ConnectionService connectionService; // module boundary: interface only
    private final QueryExecutionService queryExecutionService; // module boundary: interface only
    private final ChatMessageRepository chatMessageRepository;
    private final AiResponseCache responseCache;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(
            ResilientAiProvider aiProvider,
            ConnectionService connectionService,
            QueryExecutionService queryExecutionService,
            ChatMessageRepository chatMessageRepository,
            AiResponseCache responseCache,
            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.connectionService = connectionService;
        this.queryExecutionService = queryExecutionService;
        this.chatMessageRepository = chatMessageRepository;
        this.responseCache = responseCache;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public AskResponse ask(UUID workspaceId, UUID connectionId, UUID userId, AskRequest request) {
        var cached = responseCache.get(connectionId, request.question());
        if (cached.isPresent()) {
            try {
                return objectMapper.readValue(cached.get(), AskResponse.class);
            } catch (Exception ignored) {
                // fall through to a fresh generation on any cache deserialization issue
            }
        }

        SchemaSnapshot schema = connectionService.getSchema(workspaceId, connectionId);
        String schemaContext = renderSchemaForPrompt(schema);

        String rawSql;
        try {
            rawSql = aiProvider.generateSql(schemaContext, request.question());
        } catch (Exception e) {
            persistChat(workspaceId, connectionId, userId, request.question(), null, null,
                    ChatStatus.ERROR, "AI provider error: " + e.getMessage());
            return new AskResponse(false, null, java.util.List.of(), java.util.List.of(), 0, null,
                    "AI provider error: " + e.getMessage(), aiProvider.activeProviderName());
        }

        if (rawSql.trim().toUpperCase().startsWith(REJECT_PREFIX)) {
            String reason = rawSql.trim().substring(REJECT_PREFIX.length()).trim();
            persistChat(workspaceId, connectionId, userId, request.question(), null, null,
                    ChatStatus.REJECTED, reason);
            return new AskResponse(false, null, java.util.List.of(), java.util.List.of(), 0, null,
                    reason, aiProvider.activeProviderName());
        }

        // Even AI-generated SQL is never trusted: it goes through the exact
        // same SafeExecutionEngine pipeline as manual SQL (memory.md §2).
        ExecuteQueryResponse execResult = queryExecutionService.execute(
                workspaceId, connectionId, userId, rawSql, QuerySource.AI);

        if (!execResult.success()) {
            persistChat(workspaceId, connectionId, userId, request.question(), rawSql, null,
                    ChatStatus.REJECTED, execResult.rejectionReason());
            AskResponse rejected = new AskResponse(false, rawSql, java.util.List.of(), java.util.List.of(), 0,
                    null, execResult.rejectionReason(), aiProvider.activeProviderName());
            return rejected;
        }

        String explanation = null;
        if (!request.executeOnly()) {
            String summary = summarize(execResult);
            explanation = aiProvider.explainResult(request.question(), rawSql, summary);
        }

        persistChat(workspaceId, connectionId, userId, request.question(), rawSql, explanation,
                ChatStatus.ANSWERED, null);

        AskResponse response = new AskResponse(true, rawSql, execResult.columns(), execResult.rows(),
                execResult.rowCount(), explanation, null, aiProvider.activeProviderName());

        try {
            responseCache.put(connectionId, request.question(), objectMapper.writeValueAsString(response));
        } catch (Exception ignored) {
            // caching is best-effort
        }
        return response;
    }

    private void persistChat(UUID workspaceId, UUID connectionId, UUID userId, String question,
            String sql, String explanation, ChatStatus status, String rejectionReason) {
        chatMessageRepository.save(new ChatMessage(
                workspaceId, connectionId, userId, question, sql, explanation, status, rejectionReason));
    }

    private String renderSchemaForPrompt(SchemaSnapshot schema) {
        StringBuilder sb = new StringBuilder();
        for (var table : schema.tables()) {
            sb.append("TABLE ").append(table.name()).append(" (");
            sb.append(String.join(", ", table.columns().stream()
                    .map(c -> c.name() + " " + c.type()).toList()));
            sb.append(")\n");
        }
        return sb.toString();
    }

    private String summarize(ExecuteQueryResponse result) {
        int sampleSize = Math.min(5, result.rows().size());
        return "columns=" + result.columns() + ", rowCount=" + result.rowCount()
                + ", sample=" + result.rows().subList(0, sampleSize);
    }
}
