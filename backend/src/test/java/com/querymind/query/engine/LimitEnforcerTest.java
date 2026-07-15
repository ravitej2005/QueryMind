package com.querymind.query.engine;

import static org.junit.jupiter.api.Assertions.*;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.Test;

class LimitEnforcerTest {

    private final LimitEnforcer enforcer = new LimitEnforcer();

    @Test
    void injectsLimitWhenMissing() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse("SELECT * FROM users");
        String result = enforcer.applyRowCap(select, 500);
        assertTrue(result.toUpperCase().contains("LIMIT 500"));
    }

    @Test
    void capsExistingLimitAboveRowCap() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse("SELECT * FROM users LIMIT 100000");
        String result = enforcer.applyRowCap(select, 500);
        assertTrue(result.toUpperCase().contains("LIMIT 500"));
        assertFalse(result.contains("100000"));
    }

    @Test
    void keepsExistingLimitBelowRowCap() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse("SELECT * FROM users LIMIT 10");
        String result = enforcer.applyRowCap(select, 500);
        assertTrue(result.toUpperCase().contains("LIMIT 10"));
    }
}
