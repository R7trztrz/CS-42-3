# CS-42-3

## Docker quick start

This project can be started with Docker Compose. The Docker setup starts three
services:

- `frontend`: React/Vite frontend on `http://localhost:5173`
- `backend`: Spring Boot backend on `http://localhost:8080`
- `db`: PostgreSQL database on `localhost:5432`

Before running Docker, copy the example Docker environment file:

```bash
cp .env.docker.example .env
```

Update `.env` if real local secrets are required, especially the Turnstile
values.

Start the full project:

```bash
docker compose up --build
```

Stop the project:

```bash
docker compose down
```

## M4/M5 database upgrade

The shared migration sequence preserves main's V1-V10 unchanged and adds
M4/M5 as V11-V16. Databases following the original main history can upgrade
normally after backup and validation.

Older `feat/m5-backend` databases used V10 for the question bank and V16 for
template synchronization. Do **not** rebuild against those databases with
the new migration files: the histories differ. See
[the migration upgrade guide](M4_M5_Migration_Upgrade_Guide.md) for the
backup, isolated-copy migration, verification, and cutover workflow. Never
disable Flyway validation or delete volumes to bypass this mismatch.

## Server-style demonstration

For a non-production demonstration, use the explicit override:

```bash
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml up -d --build
```

Configure the required database, JWT, and Turnstile values in the ignored
`.env.server`. Set `APP_PARTICIPANT_BASE_URL` to the actual browser-facing URL
if it is not `http://localhost:8088`. Use one project name consistently;
`survey-demo` uses separate volumes, so it does not automatically contain
accounts or studies from your existing local project.

This override enables the `dev`-only M6 completion gate. Consent is still a
non-production placeholder, and real collection completion is not validated.
Do not use it for real participant research. The unmodified server
configuration remains fail-closed until a real M6 adapter is supplied.
