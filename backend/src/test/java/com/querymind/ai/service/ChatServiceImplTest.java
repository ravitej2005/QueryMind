package com.querymind.ai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querymind.ai.cache.AiResponseCache;
import com.querymind.ai.dto.AskRequest;
import com.querymind.ai.dto.AskResponse;
import com.querymind.ai.provider.ResilientAiProvider;
import com.querymind.ai.repository.ChatMessageRepository;
import com.querymind.connection.schema.SchemaModels.SchemaSnapshot;
import com.querymind.connection.service.ConnectionService;
import com.querymind.query.domain.QuerySource;
import com.querymind.query.dto.ExecuteQueryResponse;
import com.querymind.query.service.QueryExecutionService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contract tests against mocked AiProvider/dependencies — CI never calls a
 * real, billable AI API (rules.md §8).
 */
class ChatServiceImplTest {

    private ResilientAiProvider aiProvider;
    private ConnectionService connectionService;
    private QueryExecutionService queryExecutionService;
    private ChatMessageRepository chatMessageRepository;
    private AiResponseCache responseCache;
    private ChatServiceImpl chatService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID connectionId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        aiProvider = mock(ResilientAiProvider.class);
        connectionService = mock(ConnectionService.class);
        queryExecutionService = mock(QueryExecutionService.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        responseCache = mock(AiResponseCache.class);
        when(responseCache.get(any(), any())).thenReturn(Optional.empty());
        when(connectionService.getSchema(any(), any())).thenReturn(new SchemaSnapshot(List.of()));
        when(aiProvider.activeProviderName()).thenReturn("gemini");
        chatService = new ChatServiceImpl(
                aiProvider, connectionService, queryExecutionService, chatMessageRepository,
                responseCache, new ObjectMapper());
    }

    @Test
    void aiGeneratedSqlStillGoesThroughSafeExecutionEngine() {
        when(aiProvider.generateSql(any(), any())).thenReturn("SELECT * FROM users");
        when(queryExecutionService.execute(eq(workspaceId), eq(connectionId), eq(userId),
                eq("SELECT * FROM users"), eq(QuerySource.AI)))
                .thenReturn(new ExecuteQueryResponse(true, List.of("id"), List.of(), 0, 5, null));
        when(aiProvider.explainResult(any(), any(), any())).thenReturn("There are no users yet.");

        AskResponse response = chatService.ask(workspaceId, connectionId, userId,
                new AskRequest("how many users do we have", false));

        assertTrue(response.success());
        verify(queryExecutionService).execute(workspaceId, connectionId, userId,
                "SELECT * FROM users", QuerySource.AI);
    }

    @Test
    void aTrickQuestionThatProducesAWriteIsBlockedNotSilentlyRetried() {
        when(aiProvider.generateSql(any(), any())).thenReturn("DELETE FROM users");
        when(queryExecutionService.execute(any(), any(), any(), eq("DELETE FROM users"), eq(QuerySource.AI)))
                .thenReturn(new ExecuteQueryResponse(false, List.of(), List.of(), 0, 0,
                        "This looks like a write operation and has been blocked."));

        AskResponse response = chatService.ask(workspaceId, connectionId, userId,
                new AskRequest("delete all the users", false));

        assertFalse(response.success());
        assertNotNull(response.rejectionReason());
        verify(aiProvider, never()).explainResult(any(), any(), any());
    }

    @Test
    void modelSelfRejectionIsSurfacedWithoutExecution() {
        when(aiProvider.generateSql(any(), any()))
                .thenReturn("REJECT: This requires a write operation which is not supported.");

        AskResponse response = chatService.ask(workspaceId, connectionId, userId,
                new AskRequest("delete all the users", false));

        assertFalse(response.success());
        verify(queryExecutionService, never()).execute(any(), any(), any(), any(), any());
    }

    @Test
    void repeatedIdenticalQuestionHitsCache() {
        String cachedJson = """
            {"success":true,"generatedSql":"SELECT 1","columns":[],"rows":[],"rowCount":0,
             "explanation":"cached","rejectionReason":null,"provider":"gemini"}""";
        when(responseCache.get(eq(connectionId), eq("cached question"))).thenReturn(Optional.of(cachedJson));

        chatService.ask(workspaceId, connectionId, userId, new AskRequest("cached question", false));

        verify(aiProvider, never()).generateSql(any(), any());
    }
}
