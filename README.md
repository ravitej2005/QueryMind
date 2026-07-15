# QueryMind

AI-powered data analytics platform — ask a database a question in plain
English, see the SQL, the result, a chart, and a plain-language explanation.

## Stack
- Backend: Spring Boot (modular monolith) + Spring Security + JPA + JWT + Redis + MySQL
- Frontend: React (JavaScript, no TypeScript) + Vite + Tailwind
- Infra: Docker + Docker Compose + Nginx

## Local development

```bash
cp .env.example .env   # fill in real secrets (see .env.example)
docker compose up --build
```

Frontend: http://localhost — Backend: http://localhost:8080

### Demo business database (automatic)

QueryMind's own app database (`querymind`) stores only users, workspaces,
encrypted connection metadata, and chat history — never your business data.
To have something realistic to actually *ask questions about*, on first
`docker compose up` MySQL automatically loads a separate e-commerce demo
database (`querymind_demo`, ~127k rows across products/orders/customers/
suppliers/etc. — see `seed-data/`) and creates a read-only user scoped to
**that database only**:

| Field    | Value               |
|----------|---------------------|
| Host     | `mysql` (Docker service name) or `localhost` from the host |
| Port     | `3306`              |
| Database | `querymind_demo`    |
| User     | `qm_reader`         |
| Password | `qm_reader_pass`    |

Add this as a **Data Source** in the QueryMind UI to try the Ask/SQL Editor
features against realistic data. `qm_reader` has `SELECT`/`SHOW VIEW` only,
and cannot see QueryMind's own `querymind` database at all — by design, so
the demo never accidentally queries QueryMind's own implementation tables.

> **Note:** The init scripts (`infra/dev/`, `seed-data/schema.sql`,
> `seed-data/seed_data.sql`) only run when the MySQL data volume does not yet
> exist. To re-seed from scratch, run `docker compose down -v` first. First
> boot takes a bit longer than usual while ~127k demo rows load — the mysql
> healthcheck's `start_period` accounts for this.

### AI (Gemini)

Set `GEMINI_API_KEY` in your `.env` file with a key from
[Google AI Studio](https://aistudio.google.com/). The free tier is sufficient.
The default model is `gemini-2.5-flash`.

See `SETUP_TODO.md` for the full list of things you need to configure
yourself before this runs end-to-end.
