package com.querymind.ai.provider;

/**
 * Shared prompt construction, vendor-agnostic (used by every AiProvider
 * adapter so prompt quality doesn't drift per-provider).
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    public static String sqlGenerationPrompt(String schemaContext, String question) {
        return """
            You are a MySQL SQL generator. You are given a database schema and a
            natural-language question. Respond with ONLY a single, syntactically
            valid MySQL SELECT statement that answers the question — no
            explanation, no markdown fences, no semicolon-separated statements.

            Rules:
            - Only ever produce a SELECT statement. Never produce INSERT, UPDATE,
              DELETE, DROP, ALTER, CREATE, TRUNCATE, or any DDL/DML.
            - If the question requires a write/mutating operation, or cannot be
              answered from the given schema, respond with exactly:
              REJECT: <short reason>

            Schema:
            %s

            Question: %s

            SQL:
            """.formatted(schemaContext, question);
    }

    public static String explanationPrompt(String question, String sql, String resultSummary) {
        return """
            You explain SQL query results in plain, non-technical English for a
            business user. Be concise (2-4 sentences). Reference concrete numbers
            from the result. Do not repeat the SQL back verbatim.

            Original question: %s
            SQL that was run: %s
            Result summary (columns + sample rows + row count): %s

            Explanation:
            """.formatted(question, sql, resultSummary);
    }
}
