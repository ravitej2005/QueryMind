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

### Dev read-only database user (automatic)

On first `docker compose up`, MySQL automatically runs
`infra/dev/init-dev-db-user.sh` which creates a development read-only user:

| Field    | Value            |
|----------|------------------|
| Host     | `mysql` (Docker service name) or `localhost` if connecting from host |
| Port     | `3306`           |
| Database | `querymind`      |
| User     | `qm_reader`      |
| Password | `qm_reader_pass` |

This user has `SELECT` and `SHOW VIEW` privileges only, which is exactly what
QueryMind's connection security check requires. Use these credentials when
creating a connection inside the QueryMind UI to test the AI chat.

> **Note:** The init script only runs when the MySQL data volume does not yet
> exist. If you need to recreate it, run `docker compose down -v` first.

### AI (Gemini)

Set `GEMINI_API_KEY` in your `.env` file with a key from
[Google AI Studio](https://aistudio.google.com/). The free tier is sufficient.
The default model is `gemini-2.5-flash`.

See `SETUP_TODO.md` for the full list of things you need to configure
yourself before this runs end-to-end.
