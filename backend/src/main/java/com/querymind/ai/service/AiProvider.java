package com.querymind.ai.service;

/**
 * Pluggable LLM vendor abstraction (memory.md §2). All LLM calls go through
 * this interface; never hardcode a vendor SDK call outside an adapter class
 * implementing this interface. Gemini is the default (free tier); Ollama is
 * the second reference implementation (local/offline demo path).
 */
public interface AiProvider {

    String name();

    /** Generates a single SQL SELECT statement (or an explanation of why one can't be produced) from NL + schema context. */
    String generateSql(String schemaContext, String naturalLanguageQuestion);

    /** Produces a plain-language explanation of a query result. */
    String explainResult(String question, String sql, String resultSummary);
}
