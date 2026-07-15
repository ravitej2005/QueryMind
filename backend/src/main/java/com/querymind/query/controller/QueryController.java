package com.querymind.query.controller;

import com.querymind.query.domain.QuerySource;
import com.querymind.query.dto.ExecuteQueryRequest;
import com.querymind.query.dto.ExecuteQueryResponse;
import com.querymind.query.dto.QueryHistoryResponse;
import com.querymind.query.service.QueryExecutionService;
import com.querymind.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/connections/{connectionId}/query")
public class QueryController {

    private final QueryExecutionService queryExecutionService;
    private final WorkspaceService workspaceService;

    public QueryController(QueryExecutionService queryExecutionService, WorkspaceService workspaceService) {
        this.queryExecutionService = queryExecutionService;
        this.workspaceService = workspaceService;
    }

    @Operation(summary = "Execute a manual SQL query through SafeExecutionEngine")
    @PostMapping
    public ExecuteQueryResponse execute(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId, @Valid @RequestBody ExecuteQueryRequest req) {
        workspaceService.resolveRole(workspaceId, userId);
        return queryExecutionService.execute(workspaceId, connectionId, userId, req.sql(), QuerySource.MANUAL);
    }

    @Operation(summary = "Paginated query history for a workspace")
    @GetMapping("/history")
    public List<QueryHistoryResponse> history(
            @PathVariable UUID workspaceId, @PathVariable UUID connectionId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        workspaceService.resolveRole(workspaceId, userId);
        return queryExecutionService.history(workspaceId, page, size);
    }
}
