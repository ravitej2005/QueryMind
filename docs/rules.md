# QueryMind — Project Rules

These rules are binding for any human or AI assistant contributing to this repository. When a rule and a request conflict, the rule wins unless a human explicitly overrides it in writing in the PR description.

## 1. Coding Standards

- **Backend (Java/Spring Boot):** Google Java Style via Spotless, enforced in CI. No wildcard imports. Constructor injection only (no field `@Autowired`). Prefer `record` for DTOs. Public methods on `service` interfaces must have Javadoc if their behavior isn't obvious from the name.
- **Frontend (React/JavaScript):** ESLint (eslint-plugin-react + react-hooks recommended rules) + Prettier, enforced in CI. This is a plain-JavaScript project by deliberate choice — no `.ts`/`.tsx` files, no TypeScript compiler step. Every shared component declares `propTypes`; every non-trivial function/hook carries JSDoc `@param`/`@returns` so `jsconfig.json`'s `checkJs` gives editor-level type hints. Functional components only, no class components.
- No commented-out code committed. Delete it; git history preserves it.
- No `console.log`/`System.out.println` left in committed code — use the project logger.

## 2. Git Rules

- `main` is always deployable. No direct commits to `main` — all changes via PR.
- Branch naming: `feat/<module>-<short-desc>`, `fix/<module>-<short-desc>`, `chore/<short-desc>`, `docs/<short-desc>`.
- One logical change per PR. A PR touching both the `query` module and the dashboard UI unrelated should be two PRs.
- Rebase (not merge-commit) feature branches onto `main` before merge, to keep history linear and readable.

## 3. Commit Conventions

Conventional Commits, strictly:
```
<type>(<scope>): <short summary>

[optional body]
[optional footer]
```
Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `security`.
Scope: the module name (`auth`, `query`, `ai`, `dashboard`, `frontend`, `infra`, `docs`).

Examples:
- `feat(query): add AST validation for SafeExecutionEngine`
- `fix(auth): correct refresh token rotation race condition`
- `security(connection): enforce read-only role check on connect`

## 4. File & Folder Naming

- Java: `PascalCase.java`, one public top-level type per file, filename matches type name exactly.
- React components: `PascalCase.jsx`. Hooks: `useCamelCase.js`. Utility modules: `camelCase.js`.
- Folders: `kebab-case` for multi-word frontend feature folders (`sql-editor/`), lowercase single words for backend module packages (`connection`, not `Connection` or `connections`).
- Test files mirror the file under test with a `.test.js(x)` (frontend) or `Test.java` suffix (backend, e.g. `SafeExecutionEngineTest.java` in `backend`).

## 5. API Naming

- Base path `/api/{module}`; resources plural nouns (`/api/connections`, `/api/dashboards`).
- Action endpoints (non-CRUD) are verb-suffixed and justified in a PR comment (`/api/connections/{id}/test`, `/api/chat/query`).
- Query params: `camelCase` (`?page=0&size=20&sortBy=createdAt`).
- Request/response bodies: `camelCase` JSON keys.

## 6. Database Naming

- Tables: `snake_case`, plural (`users`, `connections`, `dashboard_widgets`).
- Columns: `snake_case` (`created_at`, `connection_id`).
- Foreign keys: `<singular_referenced_table>_id` (`workspace_id`, `connection_id`).
- All tables have `id` (UUID, not auto-increment int, to avoid enumeration and to simplify future multi-instance ID generation), `created_at`, `updated_at`.
- Every migration is a new, immutable Flyway file (`V{n}__{description}.sql`) — never edit a previously-applied migration.

## 7. Branch Strategy

- Trunk-based with short-lived feature branches (target: merged within days, not weeks).
- `main` protected: requires CI green + at least a self-review checklist pass (solo project — the "review" is the checklist in §11 below, applied deliberately, not skipped).
- Tags (`v0.1.0`, etc.) cut at the end of each phase in `phases.md`.

## 8. Testing Rules

- The `query` and `ai` modules require the highest coverage in the project — any change to `SafeExecutionEngine` must include an updated/expanded "should reject" test case.
- New backend service logic: unit test the service, integration test the controller+repository path with Testcontainers where a real DB matters.
- New frontend feature: at minimum, a rendering test + a test for the primary user interaction (submit, error state).
- No PR merges with a failing or skipped test in CI. A skipped test must have a linked issue explaining why.
- Mocks are used for external AI provider calls in all automated tests — CI never calls a real, billable AI API.

## 9. Documentation Rules

- Every new module gets a short `README.md` inside its package/folder explaining its responsibility and its public service interface, in addition to being reflected in `architecture.md`.
- OpenAPI annotations are required on every controller endpoint — the generated spec is the source of truth for API docs, not a hand-maintained doc that will drift.
- Any deviation from `memory.md`/`rules.md` conventions must be documented inline with a comment explaining why, and reflected back into those files in the same PR.

## 10. Security Requirements

- No secrets, API keys, or credentials committed, ever — `.env` is gitignored, `.env.example` documents required keys with placeholder values.
- All external DB credentials encrypted at rest (AES-256-GCM); encryption key sourced from environment/secret manager only.
- All SQL execution against a user-connected database happens exclusively through the `query` module's SafeExecutionEngine validation pipeline — no exceptions, no "temporary" bypass flags, and no direct-execution code path that skips it is ever added.
- Dependency vulnerability scanning (`npm audit` / OWASP Dependency-Check or Snyk) runs in CI; high/critical findings block merge.
- Rate limiting active on `/api/auth/*` and `/api/chat/*` in every environment, including dev, so the behavior is always tested by the developer using the app normally.

## 11. Code Review Checklist (applies to every PR, solo or reviewed)

- [ ] Follows module boundary rule (no cross-module repository/entity access)
- [ ] New endpoints have OpenAPI annotations and DTO validation
- [ ] No secrets/credentials introduced or logged
- [ ] Tests added/updated and green in CI
- [ ] No `any` (TS) or raw exception leakage (Java) introduced
- [ ] Loading/empty/error states present for any new frontend view
- [ ] `memory.md`/`architecture.md` updated if the change affects a documented decision

## 12. Performance Requirements

- Any new list/table endpoint must be paginated from day one — retrofitting pagination onto a "return everything" endpoint later is not acceptable technical debt to introduce knowingly.
- Any new frequently-read, rarely-changed data path should be evaluated for Redis caching before merge, with an explicit TTL decision recorded (even if the decision is "not cached, because X").
- Bundle size / route-level code-splitting checked before adding a new heavy frontend dependency (e.g. a second charting library).

## 13. Definition of Done

A feature is Done when, and only when:
1. It matches its `phases.md` deliverable and completion checklist.
2. Tests exist and pass in CI at the level appropriate to its risk (see §8).
3. It has loading/empty/error states (frontend) or documented error responses (backend).
4. It's covered in `architecture.md` if it introduces a new module, data flow, or external dependency.
5. It introduces no new secret, credential, or logging leak.
6. If it touches `query` or `ai` modules, the "should reject" test matrix has been reviewed and extended if needed.
7. It has been manually exercised through the actual UI/API at least once, not just unit-tested in isolation.
