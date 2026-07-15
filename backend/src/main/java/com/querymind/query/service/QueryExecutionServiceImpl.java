package com.querymind.query.service;

import com.querymind.connection.service.ConnectionService;
import com.querymind.query.domain.QueryHistory;
import com.querymind.query.domain.QuerySource;
import com.querymind.query.domain.QueryStatus;
import com.querymind.query.dto.ExecuteQueryResponse;
import com.querymind.query.dto.QueryHistoryResponse;
import com.querymind.query.engine.QueryResult;
import com.querymind.query.engine.SafeExecutionEngine;
import com.querymind.query.engine.SqlRejectedException;
import com.querymind.query.repository.QueryHistoryRepository;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryExecutionServiceImpl implements QueryExecutionService {

    private final SafeExecutionEngine safeExecutionEngine;
    private final ConnectionService connectionService; // only through its service interface
    private final QueryHistoryRepository queryHistoryRepository;

    public QueryExecutionServiceImpl(
            SafeExecutionEngine safeExecutionEngine,
            ConnectionService connectionService,
            QueryHistoryRepository queryHistoryRepository) {
        this.safeExecutionEngine = safeExecutionEngine;
        this.connectionService = connectionService;
        this.queryHistoryRepository = queryHistoryRepository;
    }

    @Override
    @Transactional
    public ExecuteQueryResponse execute(
            UUID workspaceId, UUID connectionId, UUID userId, String sql, QuerySource source) {
        DataSource dataSource = connectionService.resolveDataSource(workspaceId, connectionId);

        try {
            QueryResult result = safeExecutionEngine.execute(dataSource, sql);
            queryHistoryRepository.save(new QueryHistory(
                    workspaceId, connectionId, userId, sql, QueryStatus.SUCCESS, null,
                    result.durationMs(), result.rowCount(), source));
            return new ExecuteQueryResponse(
                    true, result.columns(), result.rows(), result.rowCount(), result.durationMs(), null);
        } catch (SqlRejectedException e) {
            // Every rejection is logged — this is a security-relevant event
            // (memory.md §10 / rules.md §8/§10).
            queryHistoryRepository.save(new QueryHistory(
                    workspaceId, connectionId, userId, sql, QueryStatus.REJECTED, e.getMessage(),
                    null, null, source));
            return new ExecuteQueryResponse(false, List.of(), List.of(), 0, 0, e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<QueryHistoryResponse> history(UUID workspaceId, int page, int size) {
        return queryHistoryRepository.findAllByWorkspaceId(workspaceId, PageRequest.of(page, size))
                .map(h -> new QueryHistoryResponse(
                        h.getId(), h.getConnectionId(), h.getSqlText(), h.getStatus().name(),
                        h.getRejectionReason(), h.getDurationMs(), h.getRowCount(),
                        h.getSource().name(), h.getCreatedAt()))
                .toList();
    }
}
