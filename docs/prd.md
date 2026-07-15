# QueryMind — Product Requirements Document

## 1. Vision

QueryMind lets anyone with a database and a question get an answer — in plain English, with the SQL shown, the result visualized, and the meaning explained — without needing to already know SQL, and without trusting a black box to touch their data unsafely.

## 2. Objectives

- Demonstrate production-grade full-stack engineering: secure auth, a real security-critical subsystem (safe SQL execution), a genuinely swappable AI integration layer, caching strategy, and a polished frontend.
- Ship something a real person could plausibly use with a real (non-toy) database, not just a demo that only works on seeded sample data.
- Serve as the anchor project on a resume/portfolio, generating specific, defensible engineering conversation in interviews rather than buzzword-only surface area.

## 3. Target Users

- **Primary (real-world persona):** a small-team founder, analyst, or engineer who wants ad-hoc answers from their production/analytics database without writing SQL by hand every time, and without giving every teammate write access.
- **Primary (actual audience):** technical interviewers and hiring managers evaluating engineering depth from a portfolio project.
- **Secondary:** the developer's own future self extending the project — hence the heavy emphasis on `memory.md`/`rules.md` as durable project context.

## 4. Functional Requirements

### MVP (Phases 0–5)
- FR1: User registration, login, JWT-based session with refresh, logout, logout-everywhere.
- FR2: Workspace with roles (Owner/Editor/Viewer).
- FR3: Connect a MySQL database with encrypted, read-only-enforced credentials; test connection before saving.
- FR4: Schema introspection and a browsable schema explorer.
- FR5: Manual SQL editor with autocomplete, validation feedback, execution, and history.
- FR6: Safe SQL execution, enforced by the `query` module's SafeExecutionEngine — an internal validation-and-execution pipeline (AST validation, `SELECT`-only enforcement, `LIMIT` injection, row cap, `EXPLAIN`-based cost check, statement timeout, read-only DB role enforcement) that is the only path permitted to open a connection to a user's database. Applies identically to both manual and AI-generated SQL.
- FR7: Natural-language chat that generates SQL, executes it (through FR6), and explains the result in plain language.
- FR8: AI provider is swappable via configuration among Gemini, Ollama, OpenAI, and OpenRouter, with Gemini as the default (free-tier, no billing setup required to run the project) and a working reference implementation for at least Gemini and Ollama.
- FR9: Auto-selected chart visualization of query results (bar/line/pie/area minimum).
- FR10: Dashboards: create, add/remove/resize widgets sourced from chat or editor results, persist layout.
- FR11: Query/result and AI-response caching for performance and cost control.
- FR12: Per-user rate limiting on AI calls.

### Post-MVP (Phase 6+)
- FR13: Shared dashboards with live "changed" notifications and viewer presence (not live cursors).
- FR14: Scheduled reports with PDF export and email delivery.
- FR15: User-defined saved prompt library.

### Explicitly out of scope for this project
- Live collaborative cursors / operational-transform editing.
- Kafka-based event streaming, Kubernetes deployment, multi-region/multi-node scaling — discussed as future evolution in interviews, not built.
- Support for database engines beyond MySQL (Postgres) within the MVP window — architected to be addable, not delivered.
- Write/mutation queries generated or executed by the AI, under any configuration.

## 5. Non-Functional Requirements

- **Security:** encrypted credentials at rest, read-only enforced DB access, AST validation and execution via the `query` module's SafeExecutionEngine, XSS-resistant JWT storage, rate-limited auth and AI endpoints, structured audit logging of all query attempts including rejections.
- **Performance:** p95 API latency (excluding AI provider round-trip) under 300ms for cached paths; query result pagination for any result set; Redis cache-aside throughout.
- **Reliability:** AI provider calls wrapped in timeout/retry/circuit breaker so a flaky provider degrades gracefully (clear error) rather than hanging the request.
- **Maintainability:** enforced module boundaries (ArchUnit test in CI), documented conventions in `memory.md`/`rules.md`, OpenAPI spec kept current.
- **Portability:** entire stack runs via `docker compose up` on a single machine with no cloud-specific dependency required for local/demo use.
- **Accessibility:** WCAG AA-oriented contrast and keyboard navigation per `design.md` §11.

## 6. User Stories

- As a user, I can register and log in so that my connections and dashboards are private to me and my workspace.
- As a user, I can connect my database with a read-only credential so that I can query it without risking accidental writes.
- As a user, I can ask "what were my top products last month" and get correct SQL, a table, a chart, and a plain-English summary.
- As a user, I can see and edit the SQL the AI generated before running it, so I'm never surprised by what's executed.
- As a user, I get a clear explanation when my question would require a write/unsafe operation, instead of silence or a generic error.
- As a user, I can pin any chat or editor result to a dashboard so I don't have to re-run the same question every day.
- As a workspace editor, I can see when a teammate updates a shared dashboard so I know my view might be stale.
- As a returning user, my previously asked questions and saved queries are searchable in my history.

## 7. MVP Definition

MVP = Phases 0 through 5 in `phases.md`: auth + workspace, one connected MySQL database, safe manual + AI-driven SQL execution, chat interface with explanation, charts, and persisted dashboards. A demoable, deployable, secure product — collaboration/reports are explicitly post-MVP so the MVP isn't held hostage by lower-value features.

## 8. Future Roadmap (narrative, not committed scope)

- PostgreSQL and (later) warehouse-style read replicas as additional connection types.
- Extracting the `ai` and `query` modules into standalone services if/when independent scaling or a different runtime (e.g. Python for embeddings/RAG) becomes genuinely necessary.
- RAG-based schema retrieval for very large schemas (hundreds of tables) where stuffing the full schema into every prompt stops being viable.
- Real-time collaborative editing (OT/CRDT) if the project's scope and time budget genuinely expand.
- Kafka-based audit/event pipeline and Kubernetes deployment if operating at a scale where a single VM is no longer sufficient — discussed as the "next scale-up" story in interviews.

## 9. Risks

- **AI cost overrun:** mitigated by response caching + per-user rate limiting (FR11/FR12).
- **SQL execution safety gap:** the single highest-severity risk in the project; mitigated by building and exhaustively testing the `SafeExecutionEngine` in its own phase (Phase 3), before AI is even wired in.
- **Scope creep toward buzzword features** (live cursors, microservices, Kafka) diluting engineering depth: mitigated by explicit "out of scope" list above and by `rules.md` Definition of Done requiring justification for new major dependencies.
- **Provider API changes/deprecation:** mitigated by the `AiProvider` abstraction — a provider outage or SDK change is isolated to one adapter class.

## 10. Constraints

- Solo development, portfolio timeline — features are cut based on genuine engineering-signal value, not completeness for its own sake.
- Must run affordably on a single low-cost VM for demo purposes.
- Tech stack fixed as: React (JavaScript) + Vite + Tailwind + React Router + TanStack Query + Zustand (frontend); Spring Boot + Spring Security + Spring Data JPA + JWT + Redis + MySQL (backend); Docker + Docker Compose + Nginx (infra). Changes to this list require an explicit decision recorded in this document, not an ad-hoc substitution mid-build.

## 11. Success Metrics

- All MVP functional requirements demoable end-to-end against a real (non-trivial) MySQL database.
- Zero known way to get the AI-integration path to execute a write/mutating statement (verified by the Phase 3/4 "should reject" test matrix).
- CI green with meaningful test coverage on `query` and `ai` modules specifically (the highest-risk modules).
- A technical reviewer can read `architecture.md` and correctly predict how the system behaves under a new feature request, without reading the code — a proxy for genuine architectural clarity.
