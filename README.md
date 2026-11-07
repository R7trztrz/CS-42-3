# CS-42-3 — Social Media Research Platform

A University of Sydney COMP5703 capstone project for preparing and running controlled social-media studies. Researchers configure a feed, publish a participation link, collect session-associated data and export records for external analysis.

The platform provides:

- Researcher registration, login and ownership-based access control.
- A drag-and-drop feed editor with a blank template and seven platform styles: Facebook, Instagram, TikTok, X, Threads, Bluesky and Truth Social.
- Image uploads and editable post drafts generated from public webpage metadata.
- An irreversible study lifecycle: **Draft → Collecting → Closed**. Published content is frozen during collection.
- Anonymous participation, optional conditional questionnaires and optional webcam-based eye tracking.
- Session-associated questionnaire, gaze and interaction records, with selectable exports for one session or an entire study.

## Technology stack

| Area | Technologies |
| --- | --- |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, Craft.js, React Router, Axios |
| Eye tracking | WebGazer.js |
| Backend | Java 17, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Hibernate, JDBC, Jakarta Validation |
| Authentication | Spring Security, JWT, BCrypt, Cloudflare Turnstile |
| Storage and export | PostgreSQL 16, JSONB, Flyway, private file storage, Apache Commons CSV, ZIP |
| Webpage import | Jsoup, Apache HttpClient |
| Build and deployment | Maven Wrapper, pnpm 12.4.1, Docker Compose, Nginx |
| Verification | JUnit, Mockito, MockMvc, Testcontainers, JaCoCo, Vitest, Testing Library, Playwright, GitHub Actions |

The backend is a modular monolith: feature modules share one Spring Boot application and PostgreSQL database. Uploaded images and generated exports are stored as private files.

## Repository layout

```text
CS-42-3/
├── SurveyPlatformBackend/      # Spring Boot application and Flyway migrations
├── SurveyPlatformFrontend/     # React application and feed templates
├── docker-compose.yml          # Local containers on ports 5173, 8080 and 5432
├── docker-compose.server.yml   # Nginx entry point on port 8088 by default
├── .env.docker.example         # Local Docker configuration template
└── .env.server.example         # Nginx deployment configuration template
```

## Prerequisites

Choose either source startup or Docker startup.

| Startup method | Requirements |
| --- | --- |
| From source | Git, JDK 17 with `JAVA_HOME` configured, Node.js 24, pnpm 12.4.1 and a running PostgreSQL 16 instance |
| With Docker | Git and Docker Engine with Compose v2, or Docker Desktop with its engine running |

Node.js 24 matches the frontend CI environment; the existing frontend Dockerfiles use Node.js 22. Maven is supplied by the backend Wrapper. Initial dependency downloads, container builds and Turnstile verification require internet access.

Clone the repository and open its root directory:

```powershell
git clone https://github.com/R7trztrz/CS-42-3.git
cd CS-42-3
```

The commands below use PowerShell. Run each startup method from the repository root unless another directory is specified.

## Shared configuration

### JWT signing key

`JWT_SECRET` must be a standard Base64-encoded random key containing at least 32 decoded bytes. Generate a new key locally with PowerShell:

```powershell
$jwtKeyBytes = [byte[]]::new(32)
$jwtKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtKeyGenerator.GetBytes($jwtKeyBytes)
$jwtKeyGenerator.Dispose()
[Convert]::ToBase64String($jwtKeyBytes)
```

Use the generated value wherever the instructions specify `JWT_SECRET`. Keep it private and reuse it across restarts; changing it invalidates previously issued access tokens.

### Local Turnstile configuration

Registration requires a Turnstile widget token from the frontend and verification by the backend. For local development, the examples below use Cloudflare's [public always-pass test keys](https://developers.cloudflare.com/turnstile/troubleshooting/testing/):

| Setting | Local test value |
| --- | --- |
| Frontend `VITE_TURNSTILE_SITE_KEY` | `1x00000000000000000000AA` |
| Backend `TURNSTILE_SECRET` | `1x0000000000000000000000000000000AA` |

Use the test pair together. For a shared deployment, replace both with matching real keys configured for the deployment hostname. Only the public site key belongs in frontend configuration; never place a private secret in a `VITE_` variable.

## Run from source

### 1. Prepare PostgreSQL

Start PostgreSQL 16. For a fresh environment, connect as a database administrator, for example with `psql -U postgres -d postgres`, and create an application account and database:

```sql
CREATE ROLE survey_user WITH LOGIN PASSWORD 'replace-with-your-local-password';
CREATE DATABASE survey_platform OWNER survey_user;
```

Replace the password and use the same value in the backend configuration. If an appropriate database and account already exist, use those instead.

Flyway creates and upgrades the application tables automatically on backend startup; Hibernate validates the resulting schema. Flyway does not create the PostgreSQL server, login account or target database. The application account needs schema creation and read/write permissions.

### 2. Start the backend

Open a terminal in the repository root. Set each environment variable individually, replacing the password and JWT placeholder:

```powershell
cd SurveyPlatformBackend

$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/survey_platform'
$env:SPRING_DATASOURCE_USERNAME = 'survey_user'
$env:SPRING_DATASOURCE_PASSWORD = 'replace-with-your-local-password'
$env:JWT_SECRET = 'paste-the-base64-key-generated-above'
$env:TURNSTILE_SECRET = '1x0000000000000000000000000000000AA'
$env:APP_PARTICIPANT_BASE_URL = 'http://localhost:5173'
$env:SPRING_PROFILES_ACTIVE = 'default'

.\mvnw.cmd clean spring-boot:run
```

These variables apply to this terminal and its child processes. Keep the terminal open while using the backend.

Spring Boot and Maven do not automatically load `.env` files. For IntelliJ IDEA, import the same values from a private `.env` file in the Spring Boot Run Configuration and launch `SurveyPlatformBackendApplication`. If copying `SurveyPlatformBackend/.env.example`, change its legacy `SPRING_PROFILES_ACTIVE=dev` setting to `default`. The `dev` and `test` profiles bypass the integrated collection-completion gate; normal integrated startup uses `default`.

On macOS/Linux, supply the same environment variables using `export NAME='value'`, then run `./mvnw clean spring-boot:run`.

### 3. Start the frontend

Open a second terminal in the repository root:

```powershell
npm install --global pnpm@12.4.1
cd SurveyPlatformFrontend
if (-not (Test-Path -LiteralPath .env.local)) {
    Copy-Item .env.example .env.local
}
```

For a new setup, edit `SurveyPlatformFrontend/.env.local` to contain:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
```

Preserve an existing local configuration rather than overwriting it. Install dependencies and start Vite:

```powershell
pnpm install --frozen-lockfile
pnpm dev --host localhost --port 5173 --strictPort
```

Use `http://localhost:5173` consistently: the backend currently allows this origin for cross-origin requests. Restart Vite after changing frontend environment variables.

### 4. Open the application

| Page | Local URL |
| --- | --- |
| Register a researcher | [http://localhost:5173/register](http://localhost:5173/register) |
| Researcher login | [http://localhost:5173/login](http://localhost:5173/login) |
| Study management | [http://localhost:5173/studies](http://localhost:5173/studies) |
| Backend Swagger UI | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) |

There is no pre-created researcher account. Register through the frontend, then log in. Protected Swagger operations require the JWT returned by login, supplied through **Authorize**.

Publishing a study generates a frontend URL in the form `/participate/{token}`. `APP_PARTICIPANT_BASE_URL` must point to the frontend address participants can reach.

Press `Ctrl+C` in each terminal to stop the source applications.

## Docker quick start

Docker builds the frontend and backend from source and starts PostgreSQL. Host installations of Java, Node.js, pnpm and PostgreSQL are not required.

The two Compose configurations below are alternatives. Use the same configuration, environment file and project name when restarting an existing stack. A new project name creates separate data volumes and does not automatically contain accounts or studies from another stack. When switching from source startup, use a fresh terminal to avoid previously exported variables overriding Compose configuration.

### Option A: Local containers

For a new setup, copy the local template:

```powershell
if (-not (Test-Path -LiteralPath .env.docker)) {
    Copy-Item .env.docker.example .env.docker
}
```

Edit `.env.docker` before starting. Set `JWT_SECRET` to the generated key, choose a database password, and use the following local settings:

```dotenv
POSTGRES_DB=survey_platform
POSTGRES_USER=survey_user
POSTGRES_PASSWORD=replace-with-your-local-password
JWT_SECRET=paste-the-base64-key-generated-above
TURNSTILE_SECRET=1x0000000000000000000000000000000AA
VITE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
VITE_API_BASE_URL=http://localhost:8080
APP_PARTICIPANT_BASE_URL=http://localhost:5173
SPRING_PROFILES_ACTIVE=default
```

Validate and start:

```powershell
docker compose --env-file .env.docker -f docker-compose.yml config --quiet
docker compose --env-file .env.docker -f docker-compose.yml up -d --build
docker compose --env-file .env.docker -f docker-compose.yml ps
```

Open [http://localhost:5173/register](http://localhost:5173/register). This setup publishes the frontend on port **5173**, backend on **8080** and PostgreSQL on **5432**. Stop any source applications or local database using those ports before starting this stack.

Allow the backend to finish its Flyway migrations and report successful startup before registering or logging in. Container creation alone does not establish application readiness.

Inspect logs or stop the stack:

```powershell
docker compose --env-file .env.docker -f docker-compose.yml logs --tail=100 backend frontend db
docker compose --env-file .env.docker -f docker-compose.yml down
```

### Option B: Nginx entry point

This configuration serves the compiled frontend through Nginx and forwards `/backend/` requests to Spring Boot. Only the frontend port is published; backend and database traffic stays inside the Compose network.

For a new setup:

```powershell
if (-not (Test-Path -LiteralPath .env.server)) {
    Copy-Item .env.server.example .env.server
}
```

Edit `.env.server`, filling the database password, generated `JWT_SECRET`, and matching Turnstile keys. For a local run, use the public test pair above and retain:

```dotenv
WEB_BIND_ADDRESS=127.0.0.1
WEB_PORT=8088
APP_PARTICIPANT_BASE_URL=http://localhost:8088
```

Validate and start the integrated application:

```powershell
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml config --quiet
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d --build
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml ps
```

Open [http://localhost:8088/register](http://localhost:8088/register). Nginx health is available at [http://localhost:8088/healthz](http://localhost:8088/healthz); it checks Nginx availability, not the complete application workflow. Swagger UI is disabled in this configuration.

For a remote deployment, configure HTTPS and network access, real Turnstile keys, and the actual public `APP_PARTICIPANT_BASE_URL`. The default loopback binding is accessible only from the host. Webcam access requires a supported browser, camera permission and a secure context such as HTTPS or localhost.

Use the base server Compose file for normal integrated startup. `docker-compose.demo.yml` is a legacy override that enables `dev` and bypasses collection-completion checks.

Inspect logs or stop the stack:

```powershell
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml logs --tail=100 backend frontend db
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml down
```

### Persistence and updates

Both Compose setups use named volumes:

| Volume | Stored data |
| --- | --- |
| `postgres_data` | PostgreSQL database records |
| `study_assets` | Uploaded and imported images |
| `study_exports` | Generated export files, subject to the export retention policy |

`docker compose down` retains named volumes. Do not add `--volumes` or `-v` when retaining data. Changing `POSTGRES_PASSWORD` in an environment file does not change the password of a database account already stored in an existing volume.

After obtaining reviewed source changes, back up persistent data and rebuild using the same startup command and existing configuration. For example, for the server stack:

```powershell
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d --build --force-recreate backend frontend
```

Frontend `VITE_` settings are embedded during image building, so changing them requires a frontend rebuild. Backend settings are read at container startup. Keep database and image backups together; retaining a volume is not a backup.

For source startup, private images and exports default to `SurveyPlatformBackend/data/assets` and `SurveyPlatformBackend/data/exports` when launched from that directory. Override these with `ASSET_STORAGE_DIRECTORY` and `EXPORT_STORAGE_DIRECTORY` when needed. Export downloads expire after 24 hours by default; expiry does not delete source research records.

Keep real environment files, keys and generated data out of version control. Existing databases must retain their applied Flyway history.
