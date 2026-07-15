package com.querymind.ai.controller;

import com.querymind.ai.dto.AskRequest;
import com.querymind.ai.dto.AskResponse;
import com.querymind.ai.dto.ChatHistoryResponse;
import com.querymind.ai.service.ChatService;
import com.querymind.common.exception.ApiException;
import com.querymind.common.ratelimit.RedisTokenBucketRateLimiter;
import com.querymind.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connections/{connectionId}/chat")
public class ChatController {

    private final ChatService chatService;
    private final WorkspaceService workspaceService;
    private final RedisTokenBucketRateLimiter rateLimiter;

    public ChatController(
            ChatService chatService, WorkspaceService workspaceService,
            RedisTokenBucketRateLimiter rateLimiter) {
        this.chatService = chatService;
        this.workspaceService = workspaceService;
        this.rateLimiter = rateLimiter;
    }

    @Operation(summary = "Ask a natural-language question; generates SQL, runs it through SafeExecutionEngine, explains the result")
    @PostMapping
    public AskResponse ask(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody AskRequest req) {
        workspaceService.resolveRole(workspaceId, userId);

        String key = "ai:" + userId;
        if (!rateLimiter.tryConsume(key, 20, Duration.ofMinutes(1))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED",
                    "Too many AI requests, retry in " + rateLimiter.getRetryAfterSeconds(key) + "s");
        }

        return chatService.ask(workspaceId, connectionId, userId, req);
    }

    @Operation(summary = "Recent chat history for a connection, most recent first")
    @GetMapping("/history")
    public List<ChatHistoryResponse> history(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        workspaceService.resolveRole(workspaceId, userId);
        return chatService.history(workspaceId, connectionId, page, size);
    }
}
