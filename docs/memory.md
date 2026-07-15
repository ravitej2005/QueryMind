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
