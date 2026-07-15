package com.querymind.query.service;

import com.querymind.query.domain.QuerySource;
import com.querymind.query.dto.ExecuteQueryResponse;
import com.querymind.query.dto.QueryHistoryResponse;
import java.util.List;
import java.util.UUID;

/**
 * Public interface for the `query` module. `ai` module calls
 * executeValidated(...) with QuerySource.AI — it never talks to
 * SafeExecutionEngine directly, and never bypasses it (memory.md §2).
 */
public interface QueryExecutionService {

    ExecuteQueryResponse execute(
            UUID workspaceId, UUID connectionId, UUID userId, String sql, QuerySource source);

    List<QueryHistoryResponse> history(UUID workspaceId, int page, int size);
}
