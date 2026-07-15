# QueryMind — Implementation Phases

Each phase produces a **working, demoable application** — never a half-wired feature. Complexity is estimated on a S/M/L/XL scale for a solo developer.

---

## Phase 0 — Planning & Foundation Setup
**Goal:** All architecture/design decisions locked before code; empty-but-running skeleton.

**Deliverables**
- `docs/` folder with all six docs (this set) committed.
- Backend: Spring Boot project scaffolded with module package structure (empty), Flyway configured, `application.yml`/`application-dev.yml`/`application-prod.yml`.
- Frontend: Vite + React (JavaScript) scaffolded, Tailwind configured, `jsconfig.json` with `checkJs` enabled for editor-level JSDoc type hints, router shell, base layout with sidebar/topbar (no real pages yet).
- `docker-compose.yml` + `docker-compose.dev.yml` bring up mysql, redis, backend, and frontend/nginx successfully with health checks passing.
- ESLint/Prettier (frontend), Checkstyle/Spotless (backend), pre-commit hook running both.
- GitHub Actions CI: lint + build (no tests yet, nothing to test).

**Dependencies:** None.
**Complexity:** S
**Testing requirements:** CI pipeline itself is the test — must go green on an empty scaffold.
**Completion checklist**
- [ ] `docker compose up` brings up all 4 containers healthy (nginx, backend, mysql, redis)
- [ ] Frontend loads a shell UI with working nav (empty pages)
- [ ] Backend responds `200` on a health endpoint
- [ ] CI pipeline green on a trivial PR

---

## Phase 1 — Authentication & Workspace Foundation
**Goal:** A user can register, log in, and land in an empty workspace.

**Deliverables**
- `auth` module: register, login, refresh, logout; BCrypt hashing; JWT issuance (access + refresh per `architecture.md` §5).
- `workspace` module: workspace creation on signup, membership table, role enum (OWNER/EDITOR/VIEWER).
- Spring Security filter chain wired to validate JWTs and populate `SecurityContext` with user + resolved role.
- Frontend: login/register pages, auth state in Zustand (access token in memory), silent-refresh-on-load flow, protected route wrapper.
- Redis: refresh-token store + `jti` blocklist wired and tested.

**Dependencies:** Phase 0.
**Complexity:** M
**Testing requirements:** Unit tests for password hashing/token issuance; integration tests (Testcontainers MySQL) for register→login→refresh→logout flow; frontend tests for protected-route redirect behavior.
**Completion checklist**
- [ ] Register/login/logout works end-to-end through the UI
- [ ] Expired access token silently refreshes without a user-visible flicker
- [ ] Refresh token rotation + reuse-detection revocation covered by a test
- [ ] RBAC role resolves per-request, not from a stale JWT claim

---

## Phase 2 — Database Connections & Schema Introspection
**Goal:** A user can connect a real MySQL database and browse its schema.

**Deliverables**
- `connection` module: CRUD for connections, AES-256-GCM credential encryption, connection test endpoint.
- Per-connection HikariCP datasource factory with pool caps, idle eviction, and **enforced read-only DB role** (documented instructions for the user to create a read-only MySQL user, validated at connect time).
- Schema introspection (JDBC `DatabaseMetaData`) producing a normalized schema model (tables, columns, types, FKs), cached in Redis.
- Frontend: connection list/create/edit UI, schema tree explorer component.

**Dependencies:** Phase 1.
**Complexity:** M
**Testing requirements:** Integration tests against a Testcontainers MySQL instance for introspection correctness; unit tests for encryption round-trip; a test verifying a write-capable credential is rejected at connect time.
**Completion checklist**
- [ ] User can add a connection and see it validated before saving
- [ ] Schema explorer renders real tables/columns/relationships
- [ ] Credentials are never observable in logs, API responses, or the DB in plaintext
- [ ] Connection pool caps verified under a simple load test (can't exhaust with concurrent requests)

---

## Phase 3 — SafeExecutionEngine (Safety Core)
**Goal:** Arbitrary SQL can be safely run against a connection, independent of AI — this is the security-critical phase and is deliberately built and tested *before* AI is wired in, so AI is layered onto an already-safe execution path. This phase stands up **SafeExecutionEngine** as an internal service inside the `query` module, since the validation pipeline is cheapest to build correctly from the start rather than retrofitted later.

**Deliverables**
- `query` module (in the main backend): **SafeExecutionEngine**, a single internal service implementing the full validation-and-execution pipeline — JSQLParser-based AST validation (reject non-`SELECT`), automatic `LIMIT` injection, row cap enforcement, an `EXPLAIN`-based cost check, execution with a hard statement timeout against the **read-only**, per-connection `HikariDataSource`, result pagination, and query history persistence.
- Query history persistence (every execution logged: user, connection, SQL, status, duration, row count, and rejection reason if any).
- Frontend: standalone SQL Editor page (Monaco), results grid (virtualized), query history sidebar, save-query feature.

**Dependencies:** Phase 2.
**Complexity:** L
**Testing requirements:** This is the highest-coverage requirement in the project. A table-driven test suite for SafeExecutionEngine covering "should reject" SQL (DROP/DELETE/UPDATE/INSERT/multi-statement/`; --` injection attempts/stacked queries) and "should allow" SQL. Plus a load test confirming the row cap and timeout actually bound resource usage.
**Completion checklist**
- [ ] 100% of the "should reject" test matrix passes
- [ ] Row cap and timeout are enforced even for legitimately expensive `SELECT`s
- [ ] Query history correctly records every attempt including rejected ones, with the rejection reason
- [ ] Manual SQL editor is fully usable without any AI involvement (proves the engine stands alone)

---

## Phase 4 — AI Integration (NL → SQL → Explanation)
**Goal:** The core "wow" feature — chat with your database.

**Deliverables**
- `ai` module: `AiProvider` interface with **`GeminiProvider` as the default, first-built adapter** (free tier, no billing setup required to demo the project) plus **`OllamaProvider`** as a second adapter (fully local/offline demo path) — proving the abstraction with two real, working, differently-hosted providers rather than one. `AiProviderFactory`, `ResilientAiProvider` (timeout/retry/circuit breaker via Resilience4j). `OpenAiProvider`/`OpenRouterProvider` are straightforward additions afterward, not built in this phase unless time allows.
- Schema-aware prompt construction using the cached schema from Phase 2.
- NL→SQL generation, wired through the Phase 3 SafeExecutionEngine (never bypassing it).
- Result explanation generation.
- Redis: AI response caching + per-user rate limiting (token bucket).
- Frontend: Ask (chat) screen — three-pane layout per `design.md` §5, streaming/staged loading states.

**Dependencies:** Phase 3 (execution engine must exist first).
**Complexity:** L
**Testing requirements:** Contract tests against a mocked `AiProvider` (never hit a real paid API in CI); a test proving AI-generated output that fails validation is surfaced to the user with a clear reason, not silently retried into something unsafe; rate-limiter unit tests.
**Completion checklist**
- [ ] Provider swap (Gemini ↔ Ollama) requires only a config change, verified by a test running the same scenario against both
- [ ] A deliberately "trick" question (e.g. asking to delete data) produces a blocked/explained response, not a silent failure
- [ ] Repeated identical questions hit the AI response cache (verified via call-count assertion in a test)
- [ ] Rate limit returns a clear 429 with retry-after info

---

## Phase 5 — Visualization & Dashboards
**Goal:** Results become charts; charts become persisted, shareable dashboards.

**Deliverables**
- Chart-type auto-selection heuristic (based on result shape: single time column + numeric → line; low-cardinality categorical + numeric → bar/pie).
- `dashboard` module: dashboard/widget CRUD, layout persistence (react-grid-layout state serialized per dashboard).
- Frontend: dashboard list, dashboard editor (drag/resize/add widget from a saved query or chat result).

**Dependencies:** Phase 4 (widgets are typically born from a chat/editor result).
**Complexity:** M
**Testing requirements:** Unit tests for the chart-type heuristic across representative result shapes; frontend tests for widget add/remove/resize persistence.
**Completion checklist**
- [ ] A chat/editor result can be "pinned" to a dashboard in one action
- [ ] Dashboard layout persists across reloads
- [ ] At least 4 chart types render correctly from real query results

---

## Phase 6 — Collaboration (Scoped) & Notifications
**Goal:** Shared dashboards update live across sessions — explicitly *not* live cursors (see architecture review).

**Deliverables**
- `notification` module: WebSocket endpoint, Redis pub-sub backing so it works across multiple backend instances, session registry.
- Dashboard viewers receive a "this dashboard changed, refresh?" toast when another editor saves changes.
- Basic presence indicator (avatars of who's currently viewing), no live cursor tracking.

**Dependencies:** Phase 5.
**Complexity:** M
**Testing requirements:** Integration test with two simulated WebSocket clients verifying broadcast delivery; test that a dropped connection cleans up presence within the heartbeat window.
**Completion checklist**
- [ ] Two browser sessions on the same dashboard see update notifications live
- [ ] Presence indicator accurately reflects active viewers within ~30s
- [ ] Server survives a client disconnecting uncleanly without leaking presence state

---

## Phase 7 — Reports, Saved Prompts & Polish
**Goal:** Recurring value features + the portfolio-ready polish pass.

**Deliverables**
- `report` module: scheduled report generation (Quartz), PDF export (e.g. via a headless-render or a reporting lib), email delivery.
- Saved prompt library (generic, user-defined — not a pre-built departmental content set).
- Accessibility pass (keyboard nav, contrast, screen-reader chart alt views) per `design.md` §11.
- Performance pass: Lighthouse/bundle-size audit on frontend, query/index audit on backend.
- README, architecture diagrams exported as images, demo video/gif for the repo.

**Dependencies:** Phase 6.
**Complexity:** M
**Testing requirements:** Scheduled job tested with a fast-forwarded/mock clock; PDF output snapshot-tested for structural correctness (not pixel-perfect).
**Completion checklist**
- [ ] A report can be scheduled and actually arrives by email on schedule
- [ ] Accessibility checklist fully passes
- [ ] README lets a stranger run the whole project locally in under 10 minutes
