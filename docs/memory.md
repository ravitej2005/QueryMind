# QueryMind — AI Assistant Memory

This file is long-term project memory for any AI coding assistant working on QueryMind. Read this before making changes. When in doubt, follow this file over inferring intent from surrounding code.

## 1. Project Overview

QueryMind is an AI-powered data analytics platform: users connect a database, ask questions in natural language, get generated SQL + results + charts + AI explanations, and build dashboards. Built as a **modular-monolith Spring Boot backend** with an internal SafeExecutionEngine module (not extracted into a separate service), a **separate React (plain JavaScript, not TypeScript) SPA frontend**, containerized with Docker, fronted by Nginx.

This is a portfolio/resume project built to demonstrate production-quality engineering, not a startup MVP chasing feature count. When a tradeoff arises between "more features" and "the existing features are correct, tested, and secure," always choose the latter.

## 2. Important Architectural Decisions (do not silently reverse these)

- **Modular monolith, not microservices.** Modules live in `com.querymind.<module>` packages inside one Spring Boot app. Do not create separate deployable services, separate repos, or a service-discovery mechanism unless a human explicitly asks for an extraction.
- **Module boundary rule:** a module may only call another module through its `service` interface. Never import another module's `repository` or JPA `domain` entity directly. Cross-module data goes through DTOs.
- **AI provider abstraction:** all LLM calls go through the `AiProvider` interface (`ai` module). Never hardcode a specific vendor SDK call outside that module's adapter classes.
- **SQL execution is never trusted.** All SQL (manual or AI-generated) is validated by the `query` module's **SafeExecutionEngine** internal service (AST validation via JSQLParser → `SELECT`-only enforcement → `LIMIT` injection → row cap → `EXPLAIN` cost check → statement timeout → read-only database role enforcement). This is the single most important security control in the system. Never add a code path that runs untrusted SQL without passing through SafeExecutionEngine, and never weaken the validation logic.
- **Database Provider abstraction:** the `connection` module uses a pluggable `DatabaseProvider` interface, not hardcoded MySQL. `MySqlProvider` is the current implementation; `PostgresProvider`, `SqlServerProvider`, and `OracleProvider` are future adapters that require zero changes to business logic — they only add a new provider implementation. Always use the provider abstraction, never import a database driver directly outside of the provider package.
- **AI provider abstraction:** the `ai` module uses a pluggable `AiProvider` interface. **Gemini is the default** (`ai.provider: gemini`), chosen for its free tier so the project is runnable without a billing setup. Other providers (Ollama, OpenAI, OpenRouter) exist behind the same interface and are equally valid — don't assume any provider-specific behavior anywhere in shared code.
- **JWT access tokens are never stored in `localStorage` or `sessionStorage`.** Access token lives in frontend memory (Zustand, non-persisted). Refresh token is an `HttpOnly` cookie.
- **Redis is cache-only.** It is never the system of record. Every Redis read path must have a correct fallback to MySQL/live computation on a cache miss.
- **Live collaborative cursors / OT are explicitly out of scope.** "Collaboration" means shared dashboards + a WebSocket broadcast that tells clients to refetch, nothing more, unless a human explicitly expands scope.

## 3. Naming Conventions

- Java packages: lowercase, singular where representing a module (`connection`, not `connections`); classes are PascalCase; REST DTOs suffixed `Request`/`Response` (`CreateConnectionRequest`, `ConnectionResponse`).
- React components: PascalCase filenames matching the component (`ChatPanel.jsx`), hooks prefixed `use` (`useChatSession.js`).
- Zustand stores: `use<Domain>Store` (`useAuthStore`, `useChatStore`).
- API routes: `/api/{module}/{resource}` in kebab-case where multi-word (`/api/connections`, `/api/query-history`).
- Database tables: `snake_case`, plural (`users`, `connections`, `dashboards`, `query_history`).
- Environment variables: `SCREAMING_SNAKE_CASE`, prefixed by concern (`DB_HOST`, `AI_OPENAI_API_KEY`, `JWT_ACCESS_SECRET`).

## 4. Folder Conventions

See `architecture.md` §3 for the canonical tree. Do not introduce new top-level folders under `backend/src/main/java/com/querymind/` without also updating that module table in `architecture.md`.

Frontend features live under `src/features/<feature>/` and own their own components/hooks/api calls; only genuinely shared UI goes in `src/components/`.

## 5. Backend Conventions

- Controllers are thin: validate input (via `@Valid` DTOs), call one service method, map result to a response DTO. No business logic in controllers.
- Services own transactions (`@Transactional` at the service method level, never at the controller or repository level).
- Every repository method that isn't a trivial CRUD lookup gets a `@Query` with an explicit JPQL/native query — no relying on Hibernate to "figure it out" for anything performance-sensitive.
- All external DB connections (to user-connected databases) use a dedicated, separately-configured `HikariDataSource` per connection, with a hard `maximumPoolSize` cap and `connectionTimeout`, created lazily and evicted after an idle period. Never use the application's own MySQL datasource to run user/AI-generated queries.
- All new endpoints require an OpenAPI annotation (`@Operation`) — this project keeps a live OpenAPI spec, not a stale hand-written doc.

## 6. Frontend Conventions

- This is a **plain JavaScript** project (`.js`/`.jsx`), not TypeScript — do not introduce `.ts`/`.tsx` files or a TypeScript compiler step. Type-safety is instead achieved via `prop-types` on shared components and JSDoc annotations checked by `jsconfig.json`'s `checkJs` — every new shared component and non-trivial hook needs both.
- Server state (anything from the API) lives in TanStack Query, never duplicated into Zustand. Zustand is only for genuine client/UI state (active theme, sidebar collapsed, in-progress chat draft, current workspace id).
- API calls go through a single typed (via JSDoc) API client (`src/lib/api.js`) wrapping `fetch`, not scattered `fetch` calls in components.
- No prop-drilling more than two levels — reach for context or a store instead.
- Every list/table view has explicit loading (skeleton), empty, and error states — do not ship a bare spinner-only or blank-on-error screen.

## 7. Security Rules

- Never log secrets, JWTs, refresh tokens, or database credentials, even at DEBUG level.
- Never construct SQL via string concatenation anywhere in the codebase, including in AI-related code — use parameterized queries / prepared statements even for internally-constructed SQL.
- Every AI-generated SQL statement must pass `SafeExecutionEngine` validation before execution — no exceptions, no debug bypass flags left in production code paths.
- Encrypt external DB credentials at rest with AES-256-GCM; the encryption key comes from an environment variable / secret manager, never hardcoded, never committed.
- CORS is explicitly allow-listed to the known frontend origin(s) — never `*` with credentials enabled.
- Rate limit both auth endpoints (brute-force protection) and AI endpoints (cost control) via the Redis token-bucket described in `architecture.md` §7.

## 8. Performance Rules

- Any query returning a potentially large result set must be paginated (backend enforces a max page size regardless of what the client requests).
- Cache reads follow cache-aside; always handle the miss path correctly rather than assuming Redis is always warm/available.
- N+1 query patterns are a defect: use `@EntityGraph` or explicit fetch joins wherever a list endpoint would otherwise lazy-load associations per row.

## 9. Error Handling

- Backend: a single `@ControllerAdvice` global exception handler maps exceptions to a consistent error response shape:
  ```json
  { "error": { "code": "SQL_VALIDATION_REJECTED", "message": "human-readable reason", "traceId": "..." } }
  ```
- Never leak stack traces or raw exception messages from third-party libraries (including the LLM SDKs or JDBC drivers) directly to the client — map to a domain-specific error code first.
- Frontend: TanStack Query error boundaries surface the `error.code`-mapped, user-facing message; raw error objects are never rendered directly in the UI.

## 10. Logging

- Structured JSON logging (Logback + `logstash-logback-encoder`), one line per event, always includes `traceId`/`requestId` from the correlation-id filter.
- Log levels: `ERROR` for anything requiring investigation, `WARN` for rejected/blocked operations (e.g. a blocked SQL statement — this is a security-relevant event, always logged), `INFO` for lifecycle events (login, connection created, dashboard created), `DEBUG` for anything else, never enabled in prod by default.

## 11. State Management

- Server state: TanStack Query (with sane `staleTime` per resource type — schema data can be stale for minutes, chat/query results should not be silently stale).
- Client/UI state: Zustand, one store per concern, never a single mega-store.
- Never store server data that TanStack Query already owns inside a Zustand store "for convenience" — this creates dual sources of truth and stale-data bugs.

## 12. API Conventions

- REST, JSON, versioned implicitly via `/api/...` for now (no `/v1/` prefix needed until there's a genuine breaking change to ship alongside a stable v1 — don't add version-number theater before it's needed).
- Standard verbs/status codes: `POST` create (201), `GET` read (200), `PUT`/`PATCH` update (200), `DELETE` (204). No verbs in URLs (`/connections/:id/test` is the one deliberate exception — an action endpoint, clearly named).
- Pagination via `?page=&size=` query params, response includes `{content, page, size, totalElements, totalPages}`.
- All authenticated endpoints require `Authorization: Bearer <token>`; never accept auth via query string.

## 13. Things AI Must Never Change Without Explicit Instruction

- The module boundary rule (§2).
- The SafeExecutionEngine validation pipeline — no code path may execute SQL against a user-connected database without passing through it, and it never gains a debug bypass.
- JWT storage strategy (memory + HttpOnly cookie).
- The read-only enforcement on user-connected database roles.
- The chosen tech stack items listed in `prd.md` §Constraints — don't swap Redis for an in-memory cache, don't swap MySQL for another DB, don't reach for TypeScript, don't add a new major dependency (message broker, ORM, framework) without it being asked for explicitly.

## 14. Common Mistakes to Avoid

- Adding a new business-logic module folder named like a microservice (`*-service`) — follow the singular feature-name convention.
- Calling another module's repository/entity directly instead of going through its service interface.
- Executing SQL directly against a user-connected database without routing it through SafeExecutionEngine's validation pipeline — this must never happen even temporarily/in a draft PR.
- Adding `.ts`/`.tsx` files or a TypeScript build step — this project is intentionally plain JavaScript.
- Putting business logic in a React component instead of a hook/query function.
- Storing the JWT access token in `localStorage` for "simplicity."
- Introducing Kafka, Kubernetes, GraphQL, ElasticSearch, or a second database engine because it was mentioned as a "future" idea in `prd.md` — these are explicitly out of scope for implementation.

## 15. Current Implementation Status (updated by AI assistant — keep this current)

**A future session should be able to resume from this section alone.**

### Backend (Spring Boot) — code-complete through Phase 4, verified running in Docker
- `auth` module: register/login/refresh(rotation+reuse-detect)/logout, BCrypt, JWT access(memory-only client-side)+refresh(HttpOnly cookie), Redis-backed refresh store.
- `workspace` module: `WorkspaceService` interface (create-on-signup, list, per-request role resolve). Cross-module boundary respected.
- `connection` module: CRUD, AES-256-GCM credential encryption, `DatabaseProvider` abstraction (`MySqlProvider` only impl so far), read-only-credential check via `SHOW GRANTS` at connect time, lazy per-connection HikariDataSource registry w/ idle eviction, schema introspection (JDBC metadata) cached in Redis.
- `query` module: **SafeExecutionEngine** — `SqlValidator` (JSQLParser AST, SELECT-only, blocks `INTO OUTFILE`/`LOAD_FILE`/`LOAD DATA`, blocks stacked statements, **blocks all SQL comments `--`/`/* */`/`#` outright as defense-in-depth against comment-injection** — see bug history below), `LimitEnforcer` (auto-inject/cap LIMIT), EXPLAIN-based cost check, statement timeout, execution against read-only datasource, `QueryHistory` logging every attempt incl. rejection reason.
- `ai` module: `AiProvider` interface, `GeminiProvider` (default, real HTTP call, model=`gemini-2.5-flash` via `ai.gemini.model` config — update this value if Google deprecates it again), `OllamaProvider` (second real local adapter), `ResilientAiProvider` (Resilience4j timeout/retry/circuit-breaker), `ChatService` (NL question → schema-aware prompt → SQL → **always routed through QueryExecutionService/SafeExecutionEngine, never bypassed** → explanation), Redis response cache (cost control) + per-user rate limit.
- Observability: Spring Boot Actuator + Micrometer + Prometheus registry added (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` exposed; health/info also permitAll for container healthchecks). Optional Prometheus+Grafana docker-compose overlay at `infra/observability/`.
- DB migrations: V1 (auth/workspace), V2 (connections), V3 (query_history), V4 (chat_messages), **V5 (query_history add updated_at)**, **V6 (workspace_members add updated_at)**. No V7+ yet for Phase 5+ features (dashboards, reports, saved prompts) — those modules are not implemented yet.
- Tests: `SqlValidatorTest` (should-reject/should-allow matrix, see bug history), `LimitEnforcerTest`, `CredentialCipherTest`, `ChatServiceImplTest` (mocked AiProvider, no real API calls in CI). **Missing:** Testcontainers integration tests (register→login→refresh flow, schema introspection against real MySQL), ArchUnit module-boundary test (prd.md success metric).

### Security configuration note
- `SecurityConfig` defines a no-op `InMemoryUserDetailsManager` bean with zero users. This suppresses Spring Boot's `UserDetailsServiceAutoConfiguration` which would otherwise generate a default in-memory user and log a generated password at startup. QueryMind's JWT filter (`JwtAuthFilter`) sets the `SecurityContext` directly and never calls `UserDetailsService`. The bean has zero effect on the authentication flow — it only satisfies the auto-configuration check.

### Known bug history (fixed)
- **Hibernate schema-validation failure — `updated_at` missing from `query_history` and `workspace_members` (fixed):** `BaseEntity` declares both `createdAt` and `updatedAt`, but `V3__query_history.sql` and `V1__init_auth_workspace.sql` (workspace_members table only) omitted `updated_at`. Fixed by `V5__query_history_add_updated_at.sql` and `V6__workspace_members_add_updated_at.sql` — each does `ALTER TABLE ... ADD COLUMN updated_at DATETIME(6) NOT NULL` and back-fills existing rows. Original migrations were not modified (Flyway checksums). Root table audit: users (✅), workspaces (✅), workspace_members (❌→fixed V6), connections (✅), query_history (❌→fixed V5), chat_messages (✅).
- **SqlValidator comment-injection gap (fixed):** `SELECT * FROM users WHERE id = 1 -- ; DROP TABLE users` was a syntactically valid single SELECT (the `--` is a real SQL comment), so it parsed clean and passed validation — a genuine security gap, not a flaky test. Fix: `SqlValidator` now rejects any raw input containing `--`, `/* */`, or `#` outside of quoted string literals, before parsing. Test matrix expanded accordingly (`SqlValidatorTest`). No legitimate analytical query needs a SQL comment, so this is a safe blanket rule.

### Frontend (React JS + Vite + Tailwind) — verified working
- Auth (login/register, silent-refresh-on-load, protected routes), Connections (list/create with test-before-save/delete), SQL Editor (Monaco, Cmd+Enter, results table — **not yet virtualized**, a known simplification vs design.md §7), Ask/Chat (three-pane layout, example questions, explain-only toggle).
- Dashboards/Reports/Saved Prompts/Settings are honest "not built yet" empty states (not fake mockups).
- **Verified in sandbox:** `npm install`, `npm run build`, `npm run lint`, `npm test` all green (two real bugs found and fixed during verification: CSS `@import` ordering, missing ESLint ignore/Node-env config).

### Infrastructure — verified running
- Multi-stage Dockerfiles for backend (Maven→JRE, non-root, healthcheck) and frontend (npm build→nginx, SPA fallback + `/api` reverse proxy).
- `docker-compose.yml` (mysql+redis+backend+frontend, healthchecks, required-secret env guards) + `docker-compose.dev.yml` override.
- **Dev-only read-only MySQL user:** `infra/dev/init-dev-db-user.sh` is mounted into MySQL's `/docker-entrypoint-initdb.d/` in `docker-compose.yml`. On first volume creation it creates `qm_reader`@`%` with `SELECT, SHOW VIEW ON querymind.*`. Credentials: user=`qm_reader`, password=`qm_reader_pass`. This is a development convenience — remove the volume mount for production. NOT implemented via Flyway (correct — user/privilege management is infrastructure, not schema evolution).
- GitHub Actions CI (`ci.yml`): backend Spotless+verify, frontend lint+test+build, docker image builds. **Not yet run against real GitHub Actions** — Spotless will likely need one `mvn spotless:apply` pass before it's green.
- Docker Compose fully verified locally: MySQL, Redis, Flyway (V1–V6), Hibernate validation, Spring Boot startup, frontend Nginx all confirmed working.

### Seed data
- `seed-data/schema.sql` + `seed-data/generate_seed.py` (Faker-based generator) produce a **separate demo database** (`querymind_demo`) — 14 e-commerce tables (products, orders, customers, suppliers, warehouses, inventory, payments, shipments, refunds, reviews, coupons, support_tickets), ~127,000 rows, referentially consistent by construction. This is the target DB a QueryMind connection points to — entirely separate from QueryMind's own app schema (V1-V4 migrations above). Structurally validated (balanced parens/quotes, FK range logic reviewed) but **never loaded into a real running MySQL** — no DB engine available in this sandbox (package mirrors returned 404 for mysql-server/mariadb-server). User must run and confirm.

### Remaining work (not started)
- Phase 5: dashboards module (chart auto-selection, widget CRUD, react-grid-layout persistence)
- Phase 6: collaboration (WebSocket + Redis pub-sub, presence, no live cursors)
- Phase 7: reports (Quartz scheduled PDF+email), saved prompt library, accessibility pass, performance pass
- ArchUnit module-boundary enforcement test
- Testcontainers integration tests
- Results-grid virtualization in SQL Editor

## 16. Stabilization pass — product story + Ask workflow bugs (this session)

**Context:** backend/frontend builds were verified working by the user locally (`mvn clean verify`, `npm run build`). This session focused on the stabilization ask: fix the product story (QueryMind must feel like it connects to external databases, never like it queries itself) and the concretely-reported Known Issues, not new features.

### Product story fix: demo database was pointed at the wrong place
- **Root cause:** `infra/dev/init-dev-db-user.sh` created `qm_reader` scoped to `querymind` — QueryMind's **own app database** (users/workspaces/connections/chat_messages/query_history). This directly caused the "AI queries QueryMind's own system tables" problem, since that was the only realistic demo target available.
- **Fix:** Rewrote the script to grant `qm_reader` access to a **separate** `querymind_demo` database only (zero access to `querymind`). `docker-compose.yml` now mounts `seed-data/schema.sql` and `seed-data/seed_data.sql` as ordered init scripts (`01-`/`02-`) that run before the user-creation script (`03-`), auto-provisioning the ~127k-row e-commerce dataset on first boot. Bumped the mysql healthcheck's `start_period`/`retries` since loading that much data on first boot takes noticeably longer than a bare container start.
- **Terminology:** renamed "Connections" → "Data Sources" in the sidebar nav label and page copy (route path `/connections` unchanged — this is wording only, not an architecture change). Added explicit "your business data stays in your own database, QueryMind only stores encrypted connection metadata" language to the Data Sources page, the New Connection modal, and Overview.
- **Active data source visibility:** new `components/shared/ActiveDataSourceBar.jsx`, shown on both Ask and SQL Editor pages — displays which data source is active and its table count, and **warns if the connected schema is made up entirely of QueryMind's own internal table names** (a hardcoded set: users/workspaces/workspace_members/connections/query_history/chat_messages/flyway_schema_history), directing the user to connect a real business database instead.

### Ask workflow bugs (Known Issue #2)
Reviewed the full Ask flow end-to-end and fixed, in `frontend/src/features/chat/AskPage.jsx`:
1. **Race condition:** no guard existed against firing a second `askMutation.mutate()` while a prior request was still in flight (e.g. clicking two example questions quickly). Concurrent requests could resolve out of order, and the results pane always renders `messages[messages.length-1]`, so a slow first response landing after a fast second one would silently overwrite the correct answer with a stale one. Fixed with a hard `if (askMutation.isPending) return` guard at the top of `handleAsk`, and disabled the example-question buttons while pending.
2. **No error handling:** a failed request (network error, 429 rate limit, 500) previously did nothing visible — the thread just didn't update, with no indication anything happened. Added an `onError` handler that appends a visible failure entry to the thread, same shape as a rejected query.
3. **Stale thread across data source switches:** switching the connection dropdown left the previous data source's messages in the thread with no indication of the source change. Now the thread re-hydrates from that specific connection's own history whenever `connectionId` changes.
4. **No history persistence in the UI:** the backend was persisting every `ChatMessage` all along, but nothing exposed it — refreshing the page silently lost the whole conversation. Added the missing `GET /api/workspaces/{workspaceId}/connections/{connectionId}/chat/history` endpoint (`ChatService.history()`, `ChatController`, new `ChatHistoryResponse` DTO) and wired `AskPage` to hydrate from it on mount/connection-change, and to invalidate/refetch it after every successful ask. Note: `ChatMessage` doesn't persist result rows/columns, only question/SQL/explanation/status — so history entries show SQL+explanation but not a live result table; this is called out in the UI itself ("ask it again to see live results").

Same race-condition class fixed in `SqlEditorPage.jsx`'s Cmd+Enter handler (guarded on `runMutation.isPending`).

### `OverviewPage` simplified
Removed a second, independent `listWorkspaces()` fetch that duplicated `App.jsx`'s workspace-hydration effect (added by a prior session to fix the `/workspaces/null/...` bug). Both set the same store value so this wasn't actively broken, but two places deriving the same piece of state is exactly the shape of bug this stabilization pass is meant to eliminate. `OverviewPage` now only reads `currentWorkspaceId` from the store; App.jsx remains the single place that fetches and sets it.

### `/workspaces/null/...` bug — an actual second instance found and fixed
The prior session's `App.jsx` fix covers page-level API calls (all guarded with `enabled: !!workspaceId`). It did **not** cover `NewConnectionModal`, whose Test/Save mutations fire on button click with no such guard — opening the modal and clicking "Test connection" before workspace hydration completes reproduces the exact bug (`testConnection(null, ...)` → `/api/workspaces/null/connections/test`). Fixed by disabling both buttons until `workspaceId` is non-null, with a visible "Setting up your workspace…" message in the interim.

### Verification performed this session
- **Frontend:** re-ran `npm install` (the zipped `node_modules` had broken symlinks and needed a clean reinstall — not a code issue, an artifact-of-zipping issue), then `npm run build`, `npx eslint .`, `npx vitest run` — **all green** against every change described above.
- **Backend:** could not run `mvn clean verify` in this sandbox (same `repo.maven.apache.org` egress-allowlist block as prior sessions, confirmed again this session). Instead, extracted the user's own successfully-built fat jar (`target/querymind-backend-0.1.0.jar`, included in their zip) to get a real 120-jar dependency classpath, but `javac` itself wasn't installable here either (same package-mirror 404s that blocked `mysql-server`/`docker.io` in prior sessions) — so even that partial compile-check couldn't run. Verified the four changed Java files by manual read-through instead: confirmed `ChatMessage` getters (`getId`/`getQuestion`/`getGeneratedSql`/`getExplanation`/`getStatus`/`getRejectionReason`/`getCreatedAt`) match the DTO mapping in `ChatServiceImpl.history()`, and confirmed the `Page<T>.map(...).toList()` pattern used there is identical to the already-working `QueryExecutionServiceImpl.history()`. **This is a lower confidence level than an actual compile — run `mvn clean verify` locally to confirm before trusting it.**
- **Docker:** not re-verified this session (no Docker binary available, consistent with prior sessions).

### What to run locally
```bash
# Backend — confirm the new chat history endpoint actually compiles/tests clean
cd backend && mvn -B clean verify

# Frontend — already verified in sandbox, should be reliably green for you too
cd frontend && npm install && npm run build && npm run lint && npm test

# Full stack — confirm the re-scoped demo database + all Ask-workflow fixes
docker compose down -v   # wipe the volume so the new init scripts actually run
docker compose up --build
# then: register → Data Sources → add the qm_reader/querymind_demo credentials
# from the README → Ask → try switching data sources, submitting quickly twice,
# and refreshing the page mid-conversation to confirm history now persists.
```


