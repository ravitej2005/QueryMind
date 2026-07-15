package com.querymind.query.engine;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

/**
 * AST-based validation stage of SafeExecutionEngine (memory.md §2 / phases.md
 * Phase3). Rejects anything that is not a single, pure SELECT statement.
 * This is the single most important security control in the system —
 * changes here require an expanded "should reject" test case (rules.md §8).
 */
@Component
public class SqlValidator {

    private static final String[] FORBIDDEN_KEYWORDS = {
        "INTO OUTFILE", "INTO DUMPFILE", "LOAD_FILE", "LOAD DATA"
    };

    public record ValidationResult(Select select, String normalizedSql) {}

    public ValidationResult validate(String rawSql) {
        if (rawSql == null || rawSql.isBlank()) {
            throw new SqlRejectedException("Empty statement");
        }

        String upper = rawSql.toUpperCase();
        for (String forbidden : FORBIDDEN_KEYWORDS) {
            if (upper.contains(forbidden)) {
                throw new SqlRejectedException(
                        "Statement blocked: contains file-system operation (" + forbidden + ")");
            }
        }

        Statement statement;
        try {
            // CCJSqlParserUtil.parse requires the input to be exactly one
            // statement followed by EOF (optionally with a trailing `;`).
            // Stacked queries ("SELECT 1; DROP TABLE x") fail to parse here.
            statement = CCJSqlParserUtil.parse(rawSql);
        } catch (JSQLParserException e) {
            throw new SqlRejectedException("Statement could not be parsed as valid SQL: could not verify it is a single safe SELECT");
        }

        if (!(statement instanceof Select select)) {
            throw new SqlRejectedException(
                    "This looks like a write operation and has been blocked. Only SELECT statements are allowed.");
        }

        // Defense in depth: reject a Select whose body still contains a
        // semicolon (shouldn't be reachable given the parser above, but
        // guards against future JSQLParser leniency changes).
        String rendered = select.toString();
        if (rendered.contains(";")) {
            throw new SqlRejectedException("Multi-statement input is not allowed");
        }

        return new ValidationResult(select, rendered);
    }
}
