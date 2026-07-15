# query module

Responsibility: **SafeExecutionEngine** — the single most important security
control in the system. Pipeline: JSQLParser AST validation → SELECT-only
enforcement → LIMIT injection → row cap → EXPLAIN cost check → statement
timeout → execute against the connection module's read-only DataSource.
Also owns query history (every attempt logged, including rejections).

Public service interface: `QueryExecutionService`. The `ai` module must call
this — never SafeExecutionEngine directly — and must never bypass it.
No debug flag may ever skip this pipeline (memory.md §2, rules.md §10).
