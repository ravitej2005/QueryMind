package com.querymind.query.engine;

import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

/**
 * Injects/caps a LIMIT clause so no query can return an unbounded result set
 * (phases.md Phase3: "automatic LIMIT injection, row cap enforcement").
 */
@Component
public class LimitEnforcer {

    public String applyRowCap(Select select, long rowCap) {
        if (select.getSelectBody() instanceof PlainSelect plainSelect) {
            Limit existing = plainSelect.getLimit();
            if (existing == null) {
                Limit limit = new Limit();
                limit.setRowCount(new LongValue(rowCap));
                plainSelect.setLimit(limit);
            } else if (existing.getRowCount() instanceof LongValue lv && lv.getValue() > rowCap) {
                existing.setRowCount(new LongValue(rowCap));
            }
            return plainSelect.toString();
        }
        // Set operations (UNION etc.) — JSQLParser models these differently;
        // fall back to a conservative wrapping subquery cap.
        return "SELECT * FROM (" + select + ") qm_capped LIMIT " + rowCap;
    }
}
