package com.querymind.connection.schema;

import java.util.List;

public class SchemaModels {
    public record ColumnInfo(String name, String type, boolean nullable, boolean primaryKey) {}
    public record ForeignKeyInfo(String column, String referencedTable, String referencedColumn) {}
    public record TableInfo(String name, List<ColumnInfo> columns, List<ForeignKeyInfo> foreignKeys) {}
    public record SchemaSnapshot(List<TableInfo> tables) {}
}
