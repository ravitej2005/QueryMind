package com.querymind.query.engine;

import java.util.List;
import java.util.Map;

public record QueryResult(List<String> columns, List<Map<String, Object>> rows, int rowCount, long durationMs) {}
