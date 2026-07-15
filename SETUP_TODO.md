# QueryMind — What YOU need to do

Everything below is stuff I can't do from a sandboxed environment. Go through
this in order before you demo/trust the project.

---

## 1. Install locally

- Java 21 (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Node 20+ (`node -version`) — confirmed working in sandbox already
- Docker + Docker Compose
- (Optional, Phase 4 second provider) Ollama — `ollama pull llama3.1`

## 2. Secrets — create `.env` (never commit it)

Copy `.env.example` → `.env` and fill in:

| Variable | How to generate |
|---|---|
| `JWT_ACCESS_SECRET` | `openssl rand -base64 48` |
| `JWT_REFRESH_SECRET` | `openssl rand -base64 48` (different value) |
| `CONNECTION_ENCRYPTION_KEY` | `openssl rand -base64 32` |
| `GEMINI_API_KEY` | Free tier key from Google AI Studio (aistudio.google.com) |
| `DB_PASSWORD` / `MYSQL_ROOT_PASSWORD` | pick anything for local/demo |

## 3. Verify the backend actually compiles (I couldn't — no Maven Central access in my sandbox)

```bash
cd backend
mvn -B spotless:apply   # auto-formats to Google Java Style first — run this before spotless:check
mvn -B verify            # compiles + runs all tests, including SqlValidator's "should reject" matrix
```

Fix anything that doesn't compile — I wrote this from API knowledge, not a
live compiler, so treat this as a strong draft, not guaranteed-correct code.

## 4. Verify the frontend (I *did* verify this one — build/lint/test all green)

```bash
cd frontend
npm install
npm run build
npm run lint
npm test
```
Already confirmed working in my sandbox after I fixed a CSS `@import` ordering
bug and a missing ESLint ignore/Node-env config. Should just work for you too.

## 5. Set up a real MySQL database + read-only user (Phase 2/3 demo data)

The whole point of SafeExecutionEngine is enforcing read-only access — you
need a **separate, real read-only MySQL user**, not your app's own DB user:

```sql
CREATE USER 'qm_reader'@'%' IDENTIFIED BY 'a-real-password';
GRANT SELECT ON your_demo_db.* TO 'qm_reader'@'%';
FLUSH PRIVILEGES;
```

Use `qm_reader` (not root, not your app DB user) when adding a Connection in
the UI — the "test connection" step will reject anything with write grants.

## 6. Seed demo data (you said you want this — I haven't written it yet)

I haven't generated seed SQL for a demo company/products dataset. Options:
- Tell me your preferred domain (e-commerce? SaaS metrics? something else?)
  and I'll write a `seed.sql` with realistic fake data next.
- Or point me at a schema you already have and I'll adapt.

## 7. Run the whole stack

```bash
docker compose up --build
```
Checklist (Phase 0 completion criteria):
- [ ] 4 containers healthy: mysql, redis, backend, frontend
- [ ] http://localhost loads the app shell
- [ ] http://localhost:8080/actuator/health returns 200
- [ ] Register a user, log in, add the read-only connection, ask a question

## 8. Manually exercise security-critical paths yourself

Don't just trust my unit tests (rules.md Definition-of-Done requires manual
exercise too):
- Try a few SQL injection attempts through the real running SQL Editor
- Ask the AI chat something like "delete all my customers" — confirm it's
  blocked with a clear reason, not silently retried
- Confirm a write-capable MySQL credential is rejected at "test connection"

## 9. Git

- `git init`, push to your own repo — I'm not doing git operations for you
- `.gitignore` files are in place for both `frontend/` and root `.env`; add
  one for `backend/target/` too if it's not already ignored by your global gitignore

## 10. CI

- GitHub Actions workflow is at `.github/workflows/ci.yml` — push to GitHub
  and confirm it goes green. First run will likely fail Spotless until you
  run `mvn spotless:apply` once and commit the reformatted code.

## 11. Still not built by me yet (be aware before calling this "done")

- Phase 5+ : dashboards, reports, saved prompts (UI shows honest "not built"
  placeholders, not fake mockups)
- Testcontainers integration tests (register→login→refresh flow, schema
  introspection against real MySQL)
- ArchUnit module-boundary test (prd.md success metric)
- Results-grid virtualization in SQL Editor (design.md §7 — currently a
  plain table, fine for demo data sizes, not fine for huge result sets)
- Seed data script (see §6 above)
