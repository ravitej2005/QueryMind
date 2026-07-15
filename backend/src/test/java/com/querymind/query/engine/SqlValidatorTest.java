package com.querymind.query.engine;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The highest-coverage requirement in the project (rules.md §8): every
 * change to SafeExecutionEngine/SqlValidator must keep this matrix green
 * and extend it if new bypass techniques are discovered.
 */
class SqlValidatorTest {

    private final SqlValidator validator = new SqlValidator();

    @ParameterizedTest
    @ValueSource(strings = {
        "DROP TABLE users",
        "DELETE FROM users WHERE id = 1",
        "UPDATE users SET password_hash = 'x'",
        "INSERT INTO users (email) VALUES ('x@x.com')",
        "TRUNCATE TABLE users",
        "ALTER TABLE users ADD COLUMN hacked INT",
        "CREATE TABLE evil (id INT)",
        "SELECT 1; DROP TABLE users",
        "SELECT * FROM users; DELETE FROM users",
        "SELECT * FROM users WHERE id = 1 -- ; DROP TABLE users",
        "SELECT * FROM users INTO OUTFILE '/tmp/dump.csv'",
        "SELECT LOAD_FILE('/etc/passwd')",
        "GRANT ALL PRIVILEGES ON *.* TO 'x'@'%'",
        "",
        "   ",
        "SELECT * FROM users; SELECT * FROM users",
        "UPDATE users SET email='x' WHERE id=1; SELECT 1"
    })
    void rejectsUnsafeStatements(String sql) {
        assertThrows(SqlRejectedException.class, () -> validator.validate(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM users",
        "SELECT id, email FROM users WHERE id = 1",
        "SELECT u.id, COUNT(o.id) FROM users u JOIN orders o ON o.user_id = u.id GROUP BY u.id",
        "SELECT * FROM users ORDER BY created_at DESC LIMIT 10",
        "SELECT COUNT(*) FROM users WHERE email LIKE '%@example.com'"
    })
    void allowsSafeSelectStatements(String sql) {
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    @Test
    void rejectionMessageIsActionableNotGeneric() {
        SqlRejectedException ex = assertThrows(SqlRejectedException.class,
                () -> validator.validate("DELETE FROM users WHERE id = 1"));
        assertTrue(ex.getMessage().toLowerCase().contains("write operation"));
    }
}
