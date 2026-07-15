package com.querymind.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The single most important security control in the system (memory.md §2).
 * Pipeline: AST validation -> SELECT-only enforcement -> LIMIT injection ->
 * row cap -> EXPLAIN cost check -> execute with statement timeout against a
 * read-only, per-connection DataSource -> paginated result.
 *
 * No code path may execute SQL against a user-connected database without
 * going through this class. It never gains a debug bypass (rules.md §10).
 */
@Service
public class SafeExecutionEngine {

    private final SqlValidator validator;
    private final LimitEnforcer limitEnforcer;
    private final long rowCap;
    private final int statementTimeoutSeconds;
    private final long explainRowEstimateCeiling;

    public SafeExecutionEngine(
            SqlValidator validator,
            LimitEnforcer limitEnforcer,
            @Value("${query.row-cap}") long rowCap,
            @Value("${query.statement-timeout-seconds}") int statementTimeoutSeconds,
            @Value("${query.explain-row-estimate-ceiling:5000000}") long explainRowEstimateCeiling) {
        this.validator = validator;
        this.limitEnforcer = limitEnforcer;
        this.rowCap = rowCap;
        this.statementTimeoutSeconds = statementTimeoutSeconds;
        this.explainRowEstimateCeiling = explainRowEstimateCeiling;
    }

    public QueryResult execute(DataSource dataSource, String rawSql) {
        long start = System.currentTimeMillis();

        // 1) AST validation + SELECT-only enforcement
        SqlValidator.ValidationResult validated = validator.validate(rawSql);

        // 2) LIMIT injection + row cap
        String cappedSql = limitEnforcer.applyRowCap(validated.select(), rowCap);

        try (Connection conn = dataSource.getConnection()) {
            // 3) EXPLAIN-based cost check, before running the real query
            checkEstimatedCost(conn, cappedSql);

            // 4) Execute with a hard statement timeout against the read-only datasource
            try (PreparedStatement stmt = conn.prepareStatement(cappedSql)) {
                stmt.setQueryTimeout(statementTimeoutSeconds);
                try (ResultSet rs = stmt.executeQuery()) {
                    QueryResult result = toQueryResult(rs, System.currentTimeMillis() - start);
                    return result;
                }
            }
        } catch (SqlRejectedException e) {
            throw e;
        } catch (Exception e) {
            throw new SqlRejectedException("Query execution failed: " + safeMessage(e));
        }
    }

    private void checkEstimatedCost(Connection conn, String sql) throws Exception {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("EXPLAIN " + sql)) {
            long maxRows = 0;
            while (rs.next()) {
                try {
                    maxRows = Math.max(maxRows, rs.getLong("rows"));
                } catch (Exception ignored) {
                    // Some engines/plans don't expose a "rows" column for every row type
                }
            }
            if (maxRows > explainRowEstimateCeiling) {
                throw new SqlRejectedException(
                        "Query rejected: estimated cost too high (~" + maxRows
                                + " rows scanned). Narrow your filters and try again.");
            }
        }
    }

    private QueryResult toQueryResult(ResultSet rs, long durationMs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }
        return new QueryResult(columns, rows, rows.size(), durationMs);
    }

    private String safeMessage(Exception e) {
        // Never leak raw JDBC/driver exception text verbatim to callers beyond
        // this module; the controller/service layer maps this to a domain
        // error code (rules.md §9). This is a best-effort trimmed message.
        String msg = e.getMessage();
        return msg == null ? "unknown error" : msg.split("\n")[0];
    }
}
