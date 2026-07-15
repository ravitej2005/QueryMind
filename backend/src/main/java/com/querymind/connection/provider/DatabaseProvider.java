package com.querymind.connection.provider;

import com.querymind.connection.domain.DatabaseConnection;
import javax.sql.DataSource;

/**
 * Pluggable per-engine strategy (memory.md §2). MySqlProvider is the only
 * implementation today; PostgresProvider/SqlServerProvider/OracleProvider
 * are future adapters requiring zero changes to business logic — they only
 * add a new implementation of this interface.
 */
public interface DatabaseProvider {

    com.querymind.connection.domain.DatabaseType supports();

    String buildJdbcUrl(DatabaseConnection connection);

    DataSource createDataSource(DatabaseConnection connection, String plaintextPassword);

    /**
     * Verifies the supplied credential can only read, not write. Implementation
     * queries the engine's privilege tables. Must be checked at connect-time
     * per phases.md Phase2 completion checklist.
     */
    boolean isReadOnlyCredential(DatabaseConnection connection, String plaintextPassword);
}
