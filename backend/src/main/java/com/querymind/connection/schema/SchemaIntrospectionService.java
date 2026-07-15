package com.querymind.connection.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querymind.connection.schema.SchemaModels.ColumnInfo;
import com.querymind.connection.schema.SchemaModels.ForeignKeyInfo;
import com.querymind.connection.schema.SchemaModels.SchemaSnapshot;
import com.querymind.connection.schema.SchemaModels.TableInfo;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * JDBC DatabaseMetaData-based introspection producing a normalized schema
 * model, cached in Redis (phases.md Phase2). Cache-aside: always handle a
 * miss correctly rather than assuming Redis is warm (memory.md §7).
 */
@Service
public class SchemaIntrospectionService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public SchemaIntrospectionService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public SchemaSnapshot getSchema(UUID connectionId, DataSource dataSource) {
        String cacheKey = "schema:" + connectionId;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, SchemaSnapshot.class);
            } catch (Exception ignored) {
                // fall through to live introspection on any deserialization issue
            }
        }
        SchemaSnapshot snapshot = introspect(dataSource);
        try {
            redis.opsForValue().set(cacheKey, objectMapper.writeValueAsString(snapshot), CACHE_TTL);
        } catch (Exception ignored) {
            // caching is best-effort; introspection result is still returned
        }
        return snapshot;
    }

    public void invalidate(UUID connectionId) {
        redis.delete("schema:" + connectionId);
    }

    private SchemaSnapshot introspect(DataSource dataSource) {
        List<TableInfo> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            try (ResultSet tableRs = meta.getTables(catalog, null, "%", new String[] {"TABLE"})) {
                while (tableRs.next()) {
                    String tableName = tableRs.getString("TABLE_NAME");
                    tables.add(new TableInfo(tableName, columnsFor(meta, catalog, tableName),
                            foreignKeysFor(meta, catalog, tableName)));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Schema introspection failed: " + e.getMessage(), e);
        }
        return new SchemaSnapshot(tables);
    }

    private List<ColumnInfo> columnsFor(DatabaseMetaData meta, String catalog, String table) throws Exception {
        Set<String> primaryKeys = new HashSet<>();
        try (ResultSet pkRs = meta.getPrimaryKeys(catalog, null, table)) {
            while (pkRs.next()) {
                primaryKeys.add(pkRs.getString("COLUMN_NAME"));
            }
        }
        List<ColumnInfo> columns = new ArrayList<>();
        try (ResultSet colRs = meta.getColumns(catalog, null, table, "%")) {
            while (colRs.next()) {
                String name = colRs.getString("COLUMN_NAME");
                columns.add(new ColumnInfo(
                        name, colRs.getString("TYPE_NAME"),
                        colRs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                        primaryKeys.contains(name)));
            }
        }
        return columns;
    }

    private List<ForeignKeyInfo> foreignKeysFor(DatabaseMetaData meta, String catalog, String table) throws Exception {
        List<ForeignKeyInfo> fks = new ArrayList<>();
        try (ResultSet fkRs = meta.getImportedKeys(catalog, null, table)) {
            while (fkRs.next()) {
                fks.add(new ForeignKeyInfo(
                        fkRs.getString("FKCOLUMN_NAME"),
                        fkRs.getString("PKTABLE_NAME"),
                        fkRs.getString("PKCOLUMN_NAME")));
            }
        }
        return fks;
    }
}
