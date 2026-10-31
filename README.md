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

## Integrated server-style local setup

For the integrated application with the real M6 completion gate:

```bash
docker compose -p survey-integrated --env-file .env.server -f docker-compose.server.yml up -d --build
```

Configure the required database, JWT, and Turnstile values in the ignored
`.env.server`. Set `APP_PARTICIPANT_BASE_URL` to the actual browser-facing URL
if it is not `http://localhost:8088`. Use one project name consistently;
`survey-integrated` uses separate volumes, so it does not automatically contain
accounts or studies from your existing local project.

The base configuration uses the integrated M6 completion gate. The optional
`docker-compose.demo.yml` override selects `dev` and bypasses that gate; use it
only for an explicitly requested legacy demo. Both Compose setups persist
database records, uploaded assets and export artifacts in named volumes.
Existing `.env` files that set `SPRING_PROFILES_ACTIVE=dev` must be reviewed
when updating the regular local setup; `default` enables the integrated gate.

To build and check an isolated local environment from PowerShell, run:

```powershell
.\scripts\verify-server-demo.ps1 -KeepRunning
```

The script creates temporary test credentials and an isolated Compose project,
checks the participant URL passed into the backend, frontend health, and backend
login input validation through Nginx. With `-KeepRunning` it prints the URL and
keeps the successful stack and private environment file for local use. Without
that switch it removes its temporary containers, volumes and environment file.
It requires Docker Desktop to be running and uses public Turnstile test keys.
It does not modify `.env.server` and is not a production deployment test.
See [the deployment guide](Server_Deployment_Guide.md#current-integrated-local-setup)
for updates and data retention guidance.
