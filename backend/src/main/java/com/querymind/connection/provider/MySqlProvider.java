package com.querymind.connection.provider;

import com.querymind.connection.domain.DatabaseConnection;
import com.querymind.connection.domain.DatabaseType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * Current DatabaseProvider implementation, MySQL only (prd.md §10 stack
 * constraint). Never import com.mysql.cj.* driver classes outside this
 * package (memory.md §2).
 */
@Component
public class MySqlProvider implements DatabaseProvider {

    @Override
    public DatabaseType supports() {
        return DatabaseType.MYSQL;
    }

    @Override
    public String buildJdbcUrl(DatabaseConnection connection) {
        return "jdbc:mysql://%s:%d/%s?useSSL=true&allowPublicKeyRetrieval=false"
                .formatted(connection.getHost(), connection.getPort(), connection.getDatabaseName());
    }

    @Override
    public DataSource createDataSource(DatabaseConnection connection, String plaintextPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(connection));
        config.setUsername(connection.getUsername());
        config.setPassword(plaintextPassword);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // Hard pool caps + idle eviction (memory.md §5): never let a single
        // user-connected DB exhaust resources.
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);
        config.setIdleTimeout(60_000);
        config.setConnectionTimeout(5_000);
        config.setMaxLifetime(10 * 60_000);
        config.setPoolName("qm-conn-" + connection.getId());
        config.setReadOnly(true);
        return new HikariDataSource(config);
    }

    @Override
    public boolean isReadOnlyCredential(DatabaseConnection connection, String plaintextPassword) {
        String url = buildJdbcUrl(connection);
        try (Connection conn = java.sql.DriverManager.getConnection(
                        url, connection.getUsername(), plaintextPassword);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SHOW GRANTS FOR CURRENT_USER()")) {
            while (rs.next()) {
                String grant = rs.getString(1).toUpperCase();
                if (grant.contains("ALL PRIVILEGES") || containsWritePrivilege(grant)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            throw new IllegalStateException("Could not verify credential privileges: " + e.getMessage(), e);
        }
    }

    private boolean containsWritePrivilege(String grant) {
        return grant.contains("INSERT") || grant.contains("UPDATE") || grant.contains("DELETE")
                || grant.contains("DROP") || grant.contains("ALTER") || grant.contains("CREATE")
                || grant.contains("TRUNCATE") || grant.contains("GRANT OPTION");
    }
}
