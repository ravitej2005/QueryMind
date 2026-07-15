# ai module

Responsibility: NL question -> generated SQL -> (through query module's
SafeExecutionEngine, never bypassed) -> result -> plain-language explanation.

Public interface: `ChatService`. Vendor abstraction: `AiProvider`
(GeminiProvider default, OllamaProvider second real adapter), wrapped by
`ResilientAiProvider` (Resilience4j timeout/retry/circuit-breaker).
Response caching + per-user rate limiting via Redis (cost control).

Every AI-generated SQL statement passes through QueryExecutionService exactly
like manual SQL — no exceptions, no shortcut path (memory.md §2, rules.md §10).
