# QueryMind

AI-powered data analytics platform — ask a database a question in plain
English, see the SQL, the result, a chart, and a plain-language explanation.

## Stack
- Backend: Spring Boot (modular monolith) + Spring Security + JPA + JWT + Redis + MySQL
- Frontend: React (JavaScript, no TypeScript) + Vite + Tailwind
- Infra: Docker + Docker Compose + Nginx

## Local development

```bash
cp .env.example .env   # fill in real secrets, see .env.example
docker compose up --build
```

Frontend: http://localhost — Backend: http://localhost:8080

See `SETUP_TODO.md` for the full list of things you need to configure
yourself before this runs end-to-end.
