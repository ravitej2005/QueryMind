# QueryMind — Architecture

## 1. High-Level Architecture

QueryMind is built as a **modular monolith**: a single deployable Spring Boot application holds all business modules, sitting behind Nginx and backed by MySQL and Redis. The frontend is a separate single-page React (JavaScript) application served as static assets by Nginx.

Modular monolith for the business logic, not microservices, because:
- One deployable unit for auth, workspace, connection management, AI orchestration, dashboards, reports, and notifications avoids distributed-systems failure modes (partial deploys, cross-service versioning, network calls between things that change together) with zero corresponding benefit at this system's actual load.
- Module boundaries (one entry point per module — a `*Service` interface) give the same separation-of-concerns and testability benefits microservices are usually reached for, without the operational tax.
- This is not a shortcut: `ai` remains a strong future extraction candidate (see §9), called out explicitly rather than pretended away.

SQL execution — the single most dangerous operation in the system, since it's the only code path that opens a connection to a database it does not own and runs SQL it did not author — is handled by the `query` module's **SafeExecutionEngine**, an internal service rather than a separately deployed component. It is still a real architectural boundary, just enforced in software within the module rather than by process/network isolation:
- Every SQL statement (manual or AI-generated) passes through one ordered pipeline: AST validation via JSQLParser → `SELECT`-only enforcement → automatic `LIMIT` injection → row cap → `EXPLAIN`-based cost check → statement timeout → read-only database role enforcement. No code path is permitted to run SQL against a user-connected database without passing through this pipeline.
- All external DB connections use a dedicated, per-connection `HikariDataSource` with a hard `maximumPoolSize` cap and `connectionTimeout`, created lazily and evicted after an idle period — never the application's own MySQL datasource.
- The read-only enforcement happens both at the database-role level (the credential itself is read-only, validated at connect time) and in `SafeExecutionEngine`'s own AST check, so a single missed layer isn't the only thing standing between a request and a write.
This is the kind of boundary a senior engineer draws around the single most dangerous operation in the system — not a stylistic microservice split.

```
┌─────────────────────────────────────────────────────────────────┐
│                           Client (Browser)                       │
│              React (JavaScript) SPA (Vite build, static assets)  │
└───────────────────────────────┬───────────────────────────────────┘
                                 │ HTTPS
┌───────────────────────────────▼───────────────────────────────────┐
│                              Nginx                                 │
│   - Serves frontend static build                                  │
│   - Reverse proxies /api/** to Spring Boot                        │
│   - Terminates TLS, gzip, security headers, rate limiting (basic) │
│   - Upgrades /ws/** to WebSocket                                  │
└───────────────────────────────┬───────────────────────────────────┘
                                 │
┌───────────────────────────────▼───────────────────────────────────┐
│                    Spring Boot Application (Modular Monolith)      │
│                                                                     │
│  ┌───────────┐ ┌───────────┐ ┌────────────┐ ┌──────────────────┐ │
│  │   auth    │ │connection │ │   query    │ │        ai         │ │
│  │  module   │ │  module   │ │  module    │ │      module       │ │
│  │           │ │           │ │ (contains  │ │                   │ │
│  │           │ │           │ │  SafeExec- │ │                   │ │
│  │           │ │           │ │  utionEng- │ │                   │ │
│  │           │ │           │ │  ine)      │ │                   │ │
│  └───────────┘ └───────────┘ └────────────┘ └──────────────────┘ │
│  ┌───────────┐ ┌───────────┐ ┌────────────┐ ┌──────────────────┐ │
│  │ dashboard │ │  report   │ │ workspace  │ │   notification    │ │
│  │  module   │ │  module   │ │  module    │ │  (ws) module      │ │
│  └───────────┘ └───────────┘ └────────────┘ └──────────────────┘ │
│                                                                     │
│  Cross-cutting: Spring Security filter chain, global exception     │
│  handler, request correlation-id filter, audit logging aspect      │
└──────────┬───────────────────────────────┬────────────────────────┘
           │                               │
┌──────────▼───────────┐        ┌──────────▼───────────┐
│        MySQL          │        │         Redis          │
│  QueryMind system DB   │        │  cache / rate-limit /   │
│  (users, connections,  │        │  session blocklist /    │
│  dashboards, history)  │        │  pub-sub for WS events  │
└────────────────────────┘        └─────────────────────────┘

           │ (SafeExecutionEngine, inside the `query` module,
           │  opens the per-connection read-only datasource)
┌──────────▼──────────────────────────────────────────┐
│        User-Connected Databases (MySQL/Postgres)      │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│                 AI Provider Abstraction Layer            │
│   AiProvider interface → GeminiProvider (default) /      │
│   OpenAiProvider / OllamaProvider / OpenRouterProvider    │
│   Selected via config (application.yml: ai.provider=...) │
└────────────────────────────────────────────────────────┘
```

## 2. Module Boundaries

Each module is a top-level package under `com.querymind.<module>` with this internal shape:

```
com.querymind.<module>/
  api/            -> REST controllers (only public entry point into the module)
  service/        -> business logic, interfaces + impl
  repository/     -> Spring Data JPA repositories
  domain/         -> JPA entities, value objects
  dto/            -> request/response DTOs (never leak entities across module boundaries)
  event/          -> domain events this module publishes/consumes
  config/         -> module-local Spring config
```

**Rule:** Module A may only call Module B through B's `service` interface (never B's repository, never B's entity types outside a DTO). This is enforced with an ArchUnit test (see `rules.md`). This single rule is what makes future extraction to microservices mechanical rather than a rewrite.

Modules and their responsibility:

| Module | Responsibility |
|---|---|
| `auth` | Registration, login, JWT issuance/refresh, password hashing, RBAC |
| `workspace` | Organizations, workspace membership, invitations, roles |
| `connection` | Storing encrypted external DB credentials, schema introspection, connection pooling |
| `query` | SQL validation and execution via SafeExecutionEngine (AST validation, row cap, cost check, timeout, read-only enforcement), result pagination, query history |
| `ai` | Provider abstraction, prompt construction, NL→SQL, result explanation, embeddings/RAG for schema context |
| `dashboard` | Dashboard/widget CRUD, layout persistence |
| `report` | Scheduled report generation (Phase 5+) |
| `notification` | WebSocket session registry, dashboard-update broadcast (Phase 5) |

## 3. Folder Structure

```
querymind/
  backend/
    src/main/java/com/querymind/
      auth/
      workspace/
      connection/
      query/
        api/
        service/
          SafeExecutionEngine.java   -> AST validation, row cap, EXPLAIN cost check,
                                          statement timeout, read-only enforcement
        repository/
        domain/
        dto/
        event/
        config/
      ai/
      dashboard/
      report/
      notification/
      common/            -> shared kernel: exceptions, base entities, correlation-id filter
      config/             -> global Spring config (security, CORS, OpenAPI, Redis)
    src/main/resources/
      application.yml
      application-dev.yml
      application-prod.yml
      db/migration/       -> Flyway migrations
    src/test/java/...
  frontend/
    src/
      app/                -> router, providers, layout shell
      features/
        auth/
        connections/
        chat/
        sql-editor/
        dashboards/
        reports/
      components/         -> shared design-system components
      lib/                -> api client, query-client config, utils
      store/               -> Zustand stores
    jsconfig.json          -> path aliases + editor-level JSDoc type checking (checkJs)
    vite.config.js
  docker/
    backend.Dockerfile
    frontend.Dockerfile
    nginx/
      nginx.conf
  docker-compose.yml
  docker-compose.dev.yml
  docs/
    architecture.md
    design.md
    memory.md
    phases.md
    prd.md
    rules.md
```

## 4. Request Lifecycle (example: "show top 10 customers by revenue")

1. Browser sends `POST /api/chat/query` with `{connectionId, question}` and `Authorization: Bearer <accessToken>`.
2. Nginx proxies to Spring Boot, preserving headers, adds `X-Request-Id` if absent.
3. Spring Security filter chain validates the JWT signature/expiry, loads `Authentication` into the security context.
4. `CorrelationIdFilter` binds the request id to the logging MDC for trace-able logs.
5. `ChatController` (in `ai` module) receives the request, calls `AiOrchestrationService`.
6. `AiOrchestrationService`:
   a. Fetches cached schema summary for `connectionId` from Redis (or asks `connection` module to introspect + cache it if missing).
   b. Builds a schema-aware prompt and calls the configured `AiProvider`.
   c. Receives generated SQL + explanation draft.
7. The generated SQL is handed to the `query` module's **SafeExecutionEngine**, which runs the full validation pipeline before anything executes:
   a. AST parse via JSQLParser; reject anything that isn't a single `SELECT` statement.
   b. Inject a `LIMIT` if one is absent, and enforce the row cap.
   c. Run `EXPLAIN` first; reject if estimated cost/row-scan exceeds a threshold.
   d. Execute against the **read-only**, per-connection, pooled `HikariDataSource`, with a hard statement timeout.
   e. Return the result set, or a structured rejection reason, to the caller — manual editor SQL and AI-generated SQL both go through this same pipeline, with no separate or shortened path for either.
8. Results are paginated, cached (keyed by `hash(sql + connectionId)`) in Redis with a short TTL, and returned with the AI-generated explanation.
9. Whole request/response and any SQL rejection is written to an append-only audit log table (`query` module) for the user's history and for security review.
10. Frontend renders table + auto-selected chart type via TanStack Query, which owns caching/retry on the client side.

## 5. Authentication Flow

- Registration: password hashed with BCrypt (cost factor 12), stored in `auth` module's `users` table.
- Login: `POST /api/auth/login` → on success issues:
  - **Access token** (JWT, 15 min TTL) — returned in response body, held in memory on the frontend (Zustand store, never `localStorage`).
  - **Refresh token** (opaque random token, 7 day TTL, rotated on use) — set as `HttpOnly`, `Secure`, `SameSite=Strict` cookie; hashed and stored server-side so it can be revoked.
- `POST /api/auth/refresh` reads the cookie, validates against the stored hash, rotates it (old one invalidated — refresh token reuse detection triggers a full session revoke as a compromise signal), issues a new access token.
- Logout: revokes the refresh token server-side; access token is short-lived enough that no blocklist is needed for the common case, but a Redis-backed blocklist keyed by JWT `jti` exists for explicit "log out everywhere."
- RBAC: `workspace` module attaches `role` (OWNER/EDITOR/VIEWER) claims resolved per-request from the workspace membership table, not baked into the JWT (so role changes take effect immediately without waiting for token expiry).

## 6. AI Request Flow (Provider Abstraction)

```java
public interface AiProvider {
    String name();
    SqlGenerationResult generateSql(SchemaContext schema, String question, List<ChatMessage> history);
    String explainResult(SchemaContext schema, String sql, QueryResultSummary result);
    List<Float> embed(String text); // for future RAG-based schema retrieval
}
```

- `GeminiProvider` is the **default, first-built implementation** — Gemini's free tier makes the project runnable and demoable by anyone cloning the repo without a billing setup, which matters far more for a portfolio project than it would for a real product. `OllamaProvider`, `OpenAiProvider`, and `OpenRouterProvider` implement the same interface and are added without touching `AiOrchestrationService` or any business logic.
- `AiProviderFactory` selects the active implementation from `application.yml` (`ai.provider: gemini|ollama|openai|openrouter`) — a Spring `@ConfigurationProperties`-driven bean selection, so switching providers is a config change, not a code change. This is proven, not just asserted: the Phase 4 test suite runs the identical scenario against at least two real adapters (Gemini + Ollama) to demonstrate the abstraction actually holds.
- All providers are wrapped by a `ResilientAiProvider` decorator handling: timeout, retry with backoff, and a circuit breaker (Resilience4j) so one flaky provider can't take down chat.
- All AI calls go through a Redis-backed token-bucket rate limiter per user, and a response cache keyed by `hash(schemaVersion + normalizedQuestion)` to avoid re-paying for repeated questions.
- Prompt construction always includes: table/column names + types + foreign keys (from cached schema introspection) + a strict system instruction that the model must only produce a single read-only `SELECT` statement — this is a defense-in-depth layer, not the only line of defense (the `SafeExecutionEngine` is the real enforcement point).

## 7. Redis Strategy

| Key pattern | Purpose | TTL | Invalidation |
|---|---|---|---|
| `schema:{connectionId}` | Cached schema introspection | 1 hour | Manual "refresh schema" action, or on connection edit |
| `ai:resp:{hash}` | Cached AI SQL-generation response | 15 min | TTL only |
| `query:result:{hash}` | Cached query result set | 60 sec | TTL only (results assumed to shift) |
| `ratelimit:ai:{userId}` | Token bucket for AI calls | rolling window | consumed on each request |
| `refresh:{tokenHash}` | Server-side refresh token record | 7 days | Rotated/deleted on use or logout |
| `jti:blocklist:{jti}` | Explicit "logout everywhere" blocklist | = access token TTL | Expires naturally |
| `ws:presence:{dashboardId}` | Set of active viewer sessions | 30 sec sliding | Heartbeat refresh from client |

Cache-aside pattern throughout: read cache → miss → compute → write cache. No write-through needed since QueryMind's system-of-record is always MySQL; Redis is purely an acceleration/rate-limiting layer, never the source of truth.

## 8. Docker Architecture

`docker-compose.yml` (production-shaped) defines:
- `nginx` — built from `docker/frontend.Dockerfile` multi-stage (Vite build → static files) combined with `docker/nginx/nginx.conf`; exposes 80/443.
- `backend` — multi-stage `docker/backend.Dockerfile` (Maven build stage → JRE-only runtime stage), exposes 8080 internally only. This is the only backend process in the system; the `query` module's `SafeExecutionEngine` runs inside it — there is no separate execution container.
- `mysql` — official image, named volume for data, seeded via Flyway on backend startup (not init scripts, so migrations are versioned in git).
- `redis` — official image, append-only persistence enabled for refresh-token durability across restarts.

A single Docker network (`querymind-internal`) connects `nginx`, `backend`, `mysql`, and `redis`. `mysql` and `redis` are not published to the host beyond what's needed for backend connectivity.

`docker-compose.dev.yml` overrides with bind-mounts + hot reload (Vite dev server, Spring Boot devtools) for local development, and exposes MySQL/Redis ports directly for debugging.

Both compose files share a single `.env` for secrets (never committed — `.env.example` is committed instead).

## 9. Deployment Architecture

- Single VM (e.g., a $6–12/mo cloud instance) running `docker compose up -d` is sufficient and honest for a resume project — this is explicitly called out in the PRD rather than pretending it needs a Kubernetes cluster.
- Nginx handles TLS via Let's Encrypt (Certbot sidecar or host cron).
- Backups: nightly `mysqldump` to object storage (documented as a script, doesn't need to be built as a "feature").
- CI (GitHub Actions): on PR — run backend tests (with Testcontainers MySQL/Redis, including the `SafeExecutionEngine` "should reject"/"should allow" test matrix), run frontend lint/tests, build both Docker images. On merge to `main` — additionally push images to GHCR and (optionally) SSH-deploy via `docker compose pull && up -d`.

**Remaining microservice-extraction candidate**, called out explicitly instead of built prematurely:
- `ai` module — highest CPU/latency variance, most likely to need independent scaling and a different runtime (e.g., Python for embeddings/RAG) later. Left inside the monolith for now because there is no *security* reason to isolate it today — only a possible future *scaling* reason, which is a weaker justification for paying the extraction cost now.
Everything else, including `query`/`SafeExecutionEngine`, has no compelling reason to be a separate service for this system, and forcing it would just be resume theater.

## 10. Design Decisions & Tradeoffs

| Decision | Reasoning | Tradeoff accepted |
|---|---|---|
| Modular monolith for all business logic, including SQL execution via the `query` module's SafeExecutionEngine | Keeps a single deployable unit while still enforcing a strict, ordered validation pipeline around the one operation (running untrusted-adjacent SQL against external DBs) that genuinely warrants extra scrutiny | The safety boundary is enforced in software (validation pipeline + read-only DB role) rather than OS-level process/network isolation — accepted because it keeps the system to one deployable, and the pipeline is exhaustively tested |
| JavaScript over TypeScript on the frontend | Matches the fixed project constraint; compensated with `jsconfig.json` + `checkJs` for editor-level type hints from JSDoc, and PropTypes for runtime prop validation | No compile-time type errors — mitigated, not eliminated, by the above |
| Gemini as the default/first AI provider | Free tier means the project is runnable by anyone without a billing setup — genuinely improves demoability, not just cost | Slightly different prompt/response shape per provider, handled entirely inside each adapter, never leaking into `AiOrchestrationService` |
| MySQL for both system-of-record and (initially) the only supported external DB type | One less driver/dialect to support in MVP | PostgreSQL support explicitly deferred, not abandoned |
| Read-only enforced DB role for AI-generated SQL | Removes an entire class of catastrophic failure | Users can't ask the AI to mutate data — acceptable, this is an analytics tool |
| No live cursors / OT collaboration | Enormous complexity for low resume credit if shallow | "Collaboration" scoped to shared dashboards + update broadcast instead |
| JWT access token in memory, refresh token in HttpOnly cookie | Standard mitigation against XSS token theft | Slightly more complex frontend auth bootstrap (silent refresh on load) |
| Redis is cache-only, never source of truth | Simplifies failure mode: Redis down = slower, not broken | Must ensure all code paths have a Redis-miss fallback |
