# Server Deployment Preparation and Local Validation

Repository: R7trztrz/CS-42-3
Validation date: 5 October 2026 (Australia/Sydney)
Status: Server-oriented Docker configuration validated locally. Shared-server deployment and automated CD remain pending.

## 1. Scope

This guide describes the separate `survey-server` Compose environment prepared for a future team test server. It serves the built frontend through Nginx, proxies API requests to the backend, and persists database records and uploaded assets.

Local deployment validation does not establish that all application modules are complete or that their regression tests pass. Existing module test reports remain separate.

## 2. Files

| File | Purpose | Commit |
| --- | --- | --- |
| `docker-compose.server.yml` | Separate server services, networking and volumes | Yes |
| `SurveyPlatformFrontend/Dockerfile.server` | Frontend build followed by Nginx runtime | Yes |
| `SurveyPlatformFrontend/nginx.server.conf` | Static frontend, SPA fallback and API proxy | Yes |
| `.env.server.example` | Environment variable names without secrets | Yes |
| `.env.server` | Actual environment credentials | No |
| `.gitignore` | Allow deployment configuration and template | Yes |
| `Server_Deployment_Guide.md` | Deployment instructions and validation record | Yes |

The existing local modification to `docker-compose.yml` changes the Mac database host port. Exclude that modification from this deployment commit and retain it locally.

The repository root currently uses an allowlist: `/*` ignores root entries unless explicitly allowed. Add these exceptions:

```gitignore
# Track server deployment configuration and environment template
!/docker-compose.server.yml
!/.env.server.example
!/Server_Deployment_Guide.md
```

Do not allow `.env.server`. Confirm that it is ignored before committing.

## 3. Service layout

| Service | Container port | Host binding | Persistent storage |
| --- | --- | --- | --- |
| frontend | 80 | `127.0.0.1:8088` by default | Built frontend in image |
| backend | 8080 | None | `study_assets` mounted at `/app/data/assets` |
| db | 5432 | None | `postgres_data` mounted at `/var/lib/postgresql/data` |

Browser API requests use `/backend`. Nginx removes this prefix when forwarding to `http://backend:8080/`. For example, `/backend/auth/login` becomes `/auth/login`.

Keep the Compose project name `survey-server` consistent. Its volumes are `survey-server_postgres_data` and `survey-server_study_assets`. These are separate from the original `gitrepository` environment. Accounts in one database do not automatically exist in the other.

The frontend `/healthz` endpoint checks Nginx availability. It does not prove backend or database application readiness. The backend currently has no Compose healthcheck; confirm it through logs and application operations.

## 4. Environment preparation

Run all commands from the repository root. Docker must be running.

For a new environment, create a local copy:

```bash
cp -n .env.server.example .env.server
```

Fill the following values locally:

```dotenv
# Web entry point; access through an HTTPS reverse proxy when deployed
WEB_BIND_ADDRESS=127.0.0.1
WEB_PORT=8088

POSTGRES_DB=survey_platform
POSTGRES_USER=survey_user
POSTGRES_PASSWORD=

# Standard Base64-encoded random key containing at least 32 decoded bytes
JWT_SECRET=

# Use real Cloudflare Turnstile keys for production deployment
TURNSTILE_SECRET=
VITE_TURNSTILE_SITE_KEY=
```

For the already validated Mac environment, `.env.server` was copied from the working `.env` using `cp -n .env .env.server`. Preserve its existing database password and JWT secret; do not overwrite them with the empty template.

The JWT secret must use standard Base64. The earlier backend failure `Illegal base64 character 5f` was caused by an invalid value. A fresh secret can be generated locally using `openssl rand -base64 32`; keep its output private and enter it into the local environment file.

Changing the environment's database password does not change an existing database user's password. For an existing volume, use the matching password or deliberately update the database user. Do not delete data to repair authentication.

Local Turnstile validation used the official always-pass test keys:

```dotenv
VITE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
TURNSTILE_SECRET=1x0000000000000000000000000000000AA
```

These values validate integration during local testing. They do not provide real bot protection. A shared deployment requires real matching keys configured for its hostname.

Frontend Vite variables are supplied at build time. Rebuild the frontend after changing its site key. Shell environment variables may override Compose values; clear stale exported values when diagnosing unexpected configuration. Avoid sharing an unredacted environment file or full rendered Compose configuration.

## 5. Validate and start

```bash
git check-ignore -v .env.server
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml config --quiet
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d --build
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml ps
curl -i http://127.0.0.1:8088/healthz
```

Expected results: `.env.server` is ignored; configuration validation produces no error; all three services run; database and frontend become healthy; `/healthz` returns HTTP 200 and `ok`.

Open `http://127.0.0.1:8088`. Register a test account if this database is new, then verify login, Study creation, Feed editing, image upload, saving and reopening.

Inspect recent logs when a service fails:

```bash
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml logs --since=5m --tail=100 backend frontend db
```

The backend disables SpringDoc API documentation and Swagger UI in this server configuration. That setting has not yet received a separate HTTP verification.

## 6. Restart and update

Stop containers while retaining the data volumes:

```bash
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml down
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d
```

Do not add `-v` when data must be retained. Volume retention is not a backup.

After obtaining reviewed code changes, rebuild and recreate both application services:

```bash
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d --build --force-recreate backend frontend
```

Recreating the frontend alongside the backend also refreshes Nginx's upstream resolution. Recheck container status, `/healthz`, login and saved content. Take verified backups before a shared deployment update involving database migrations. Database rollback and backup restoration are not yet implemented or tested by this preparation work.

## 7. Local validation record

Results combine inspected terminal screenshots with the tester's explicit confirmations. The assistant did not operate the Mac directly.

| Check | Result | Evidence |
| --- | --- | --- |
| Server Compose configuration | PASS | `config --quiet` completed without errors |
| Frontend and backend image build | PASS | Local server environment started successfully |
| Three services running | PASS | 20:29 screenshot shows all services up |
| Database healthcheck | PASS | 20:29 screenshot shows healthy |
| Frontend healthcheck | PASS | 20:29 screenshot shows healthy |
| Nginx `/healthz` | PASS | 20:29 screenshot shows HTTP 200 and `ok` |
| Registration and login | PASS | New server environment login confirmed earlier |
| Study and Feed save/reopen | PASS | Tester confirmed local server validation |
| Uploaded image display after refresh | PASS | Tester confirmed local server validation |
| Data persistence after `down` / `up` without `-v` | PASS | Tester confirmed account, Study, Feed and image retained |
| Secret file excluded from Git | PASS | 20:27 screenshot shows `.gitignore:2:/* .env.server` |
| Deployment configuration/template visible to Git | PASS | 20:27 screenshot lists the new files |
| Full browser regression after switching to `.env.server` | PENDING | Latest screenshot shows service checks only |
| Shared-server access and HTTPS | NOT RUN | No shared server provisioned |
| Real Turnstile protection | NOT RUN | Local testing used always-pass keys |
| Backup and restoration | NOT RUN | No restoration exercise performed |
| Automated CD | NOT IMPLEMENTED | No deployment workflow created in this work |

No aggregate functional test count is claimed for this deployment validation.

## 8. Remaining shared-server and CD work

1. Obtain a team-accessible server and agree on its administrator, hostname and deployment permissions.
2. Install and validate Docker/Compose on that server, then obtain the reviewed repository revision.
3. Create a separate server `.env.server` with unique credentials and real Turnstile keys. Keep the frontend bound to loopback when a host reverse proxy is used.
4. Configure an HTTPS reverse proxy, certificate renewal and network access. Verify forwarding of the original HTTPS scheme through both proxy layers before relying on forwarded headers. The current configuration has only been tested over local HTTP.
5. Run the startup and application checks on the actual server, including external-browser access and any camera-dependent functionality required by the integrated application.
6. Implement database and asset backups, restricted backup storage, a restore exercise, log retention and failure monitoring.
7. Agree on CD triggers and implement a deployment workflow after successful CI. Record the deployed revision, serialize deployments, protect deployment credentials, and run application checks after deployment.
8. Define rollback around both application images and database migrations. Prefer revision-tagged images retained for rollback; the current configuration builds from a local checkout and uses mutable base-image tags.

The existing test CI and a future deployment workflow serve different purposes. Adding these deployment files alone does not create automated CD.

## 9. Prepare the commit

After placing this guide at the repository root and adding its allowlist exception, stage only the intended files:

```bash
git add .gitignore .env.server.example docker-compose.server.yml SurveyPlatformFrontend/Dockerfile.server SurveyPlatformFrontend/nginx.server.conf Server_Deployment_Guide.md
git diff --cached --check
git diff --cached --name-only
git status --short
```

Expected staged files are the six paths above. The real `.env.server` must be absent. The existing local `docker-compose.yml` port modification must remain unstaged. Review the staged changes before committing; avoid `git add .` for this task.
