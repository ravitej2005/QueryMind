package com.querymind.query.engine;

/** Thrown when SafeExecutionEngine's validation pipeline rejects a statement. */
public class SqlRejectedException extends RuntimeException {
    public SqlRejectedException(String reason) {
        super(reason);
    }
}
