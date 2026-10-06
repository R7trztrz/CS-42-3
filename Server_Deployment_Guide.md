# Server Deployment Guide

Repository: R7trztrz/CS-42-3
Configuration/documentation review: 7 October 2026 (Australia/Sydney)
Status: M4/M5 demonstrations require the explicit demo override described below. Production M6 completion wiring, shared-server deployment, and automated CD remain pending.

## 1. Scope

This guide describes the separate `survey-server` Compose environment prepared for a future team test server. It serves the built frontend through Nginx, proxies API requests to the backend, and persists database records and uploaded assets.

The [regular local setup](README.md#docker-quick-start) uses `docker-compose.yml` and ports 5173/8080. It is separate from the server-style setup below; do not mix their Compose project names or environment files when updating an existing deployment.

Local deployment validation does not establish that all application modules are complete or that their regression tests pass. Existing module test reports remain separate.

## 2. Files

| File | Purpose | Versioned |
| --- | --- | --- |
| `docker-compose.server.yml` | Separate server services, networking and volumes | Yes |
| `docker-compose.demo.yml` | Explicit development-only M4/M5 demo override | Yes |
| `SurveyPlatformFrontend/Dockerfile.server` | Frontend build followed by Nginx runtime | Yes |
| `SurveyPlatformFrontend/nginx.server.conf` | Static frontend, SPA fallback and API proxy | Yes |
| `.env.server.example` | Environment variable names without secrets | Yes |
| `.env.server` | Actual environment credentials | No |
| `.gitignore` | Allow deployment configuration and template | Yes |
| `Server_Deployment_Guide.md` | Deployment instructions and dated historical validation | Yes |

The deployment configurations and example environment file are already tracked. Keep actual `.env`/`.env.server` files, database dumps, private backups and personal work records out of Git. Confirm that `.env.server` is ignored before sharing changes.

## 3. Service layout

| Service | Container port | Host binding | Persistent storage |
| --- | --- | --- | --- |
| frontend | 80 | `127.0.0.1:8088` by default | Built frontend in image |
| backend | 8080 | None | `study_assets` mounted at `/app/data/assets` |
| db | 5432 | None | `postgres_data` mounted at `/var/lib/postgresql/data` |

Browser API requests use `/backend`. Nginx removes this prefix when forwarding to `http://backend:8080/`. For example, `/backend/auth/login` becomes `/auth/login`.

Keep the Compose project name consistent with the initial deployment: `survey-demo` for the demo commands below, or `survey-server` for the intended production topology. Volume names are project-specific, such as `survey-server_postgres_data` and `survey-server_study_assets`; accounts in one project's database do not automatically exist in another. Confirm the existing project and volumes before an update rather than choosing a new name.

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

For an existing environment, preserve its private environment file and matching database credentials/JWT secret; do not overwrite them with the empty template. The Mac-specific setup from 5 October is recorded separately in Appendix A.

The JWT secret must use standard Base64. A fresh secret can be generated locally using `openssl rand -base64 32`; keep its output private and enter it into the local environment file.

Changing the environment's database password does not change an existing database user's password. For an existing volume, use the matching password or deliberately update the database user. Do not delete data to repair authentication.

For development-only Turnstile checks, the public always-pass test keys are:

```dotenv
VITE_TURNSTILE_SITE_KEY=1x00000000000000000000AA
TURNSTILE_SECRET=1x0000000000000000000000000000000AA
```

These are public test values, not private deployment credentials. They validate integration during local testing but do not provide real bot protection. A shared deployment requires real matching keys configured for its hostname.

Frontend Vite variables are supplied at build time. Rebuild the frontend after changing its site key. Shell environment variables may override Compose values; clear stale exported values when diagnosing unexpected configuration. Avoid sharing an unredacted environment file or full rendered Compose configuration.

## 5. Validate and start

### M4/M5 demonstration (not real participant research)

The current M5 backend requires `CollectionCompletionGate`. Its no-op adapter
is intentionally available only in `dev`/`test`; the base server configuration
does not supply the production M6 adapter and must not be described as
production-ready. For a demonstration, explicitly opt in:

```bash
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml config --quiet
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml up -d --build
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml ps
```

Use the same project name and both Compose files for subsequent updates.
The `survey-demo` volumes are separate from other projects; do not change the
project name when reusing an existing deployment's data. Configure
`APP_PARTICIPANT_BASE_URL` in `.env.server` for the actual browser-facing URL
when it differs from `http://localhost:8088`. The override supplies that
default and sets `SPRING_PROFILES_ACTIVE=dev` only for this demo deployment.
Consent remains unapproved placeholder text; real collection completion is
not checked. Never use this override for actual participant research.

Before any database upgrade, read
[the M4/M5 migration guide](M4_M5_Migration_Upgrade_Guide.md).
Normal main V1-V10 history is unchanged; legacy feature histories require a
separate copy-and-cutover, not renamed files against the old database.

### Production deployment boundary

The base-only commands below describe the intended production topology;
they are not a currently complete M4/M5 launch procedure. Supply and test a
real M6 completion adapter before using them. Do not remove the profile
restriction or silently enable the no-op gate in production.

```bash
git check-ignore -v .env.server
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml config --quiet
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml up -d --build
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml ps
curl -i http://127.0.0.1:8088/healthz
```

With a valid completion adapter, expected results are: `.env.server` is ignored; configuration validation produces no error; all three services run; database and frontend become healthy; `/healthz` returns HTTP 200 and `ok`. For the current demo-only adapter, use both Compose files above.

Open `http://127.0.0.1:8088`. Register a test account if this database is new, then verify login, Study creation, Feed editing, image upload, saving and reopening.

Inspect recent logs when a service fails:

```bash
docker compose -p survey-server --env-file .env.server -f docker-compose.server.yml logs --since=5m --tail=100 backend frontend db
```

The backend disables SpringDoc API documentation and Swagger UI in this server configuration. That setting has not yet received a separate HTTP verification.

## 6. Restart and update

Use the same project name, environment file and Compose files used at initial startup. The following commands are for the **M4/M5 demo** in section 5 and retain both data volumes:

```bash
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml down
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml up -d
```

Do not add `-v` when data must be retained. Volume retention is not a backup.

After obtaining reviewed code changes, rebuild and recreate both application services:

```bash
docker compose -p survey-demo --env-file .env.server -f docker-compose.server.yml -f docker-compose.demo.yml up -d --build --force-recreate backend frontend
```

For an already provisioned production environment with a real M6 adapter, use its existing `survey-server` project and base server configuration instead. Never switch a demo to the base-only commands merely to restart it, and never enable the demo no-op for real participant research.

Recreating the frontend alongside the backend also refreshes Nginx's upstream resolution. Recheck container status, `/healthz`, login and saved content. Take verified backups before an update involving database migrations; follow the migration guide and validate the target environment's recovery procedure. The historical checks in Appendix A did not establish production backup or rollback readiness.

## 7. Remaining shared-server and CD work

1. Obtain a team-accessible server and agree on its administrator, hostname and deployment permissions.
2. Install and validate Docker/Compose on that server, then obtain the reviewed repository revision.
3. Create a separate server `.env.server` with unique credentials and real Turnstile keys. Keep the frontend bound to loopback when a host reverse proxy is used.
4. Configure an HTTPS reverse proxy, certificate renewal and network access. Verify forwarding of the original HTTPS scheme through both proxy layers before relying on forwarded headers. The current configuration has only been tested over local HTTP.
5. Run the startup and application checks on the actual server, including external-browser access and any camera-dependent functionality required by the integrated application.
6. Implement database and asset backups, restricted backup storage, a restore exercise, log retention and failure monitoring.
7. Agree on CD triggers and implement a deployment workflow after successful CI. Record the deployed revision, serialize deployments, protect deployment credentials, and run application checks after deployment.
8. Define rollback around both application images and database migrations. Prefer revision-tagged images retained for rollback; the current configuration builds from a local checkout and uses mutable base-image tags.

The existing test CI and a future deployment workflow serve different purposes. Adding these deployment files alone does not create automated CD.

## Appendix A. Historical local validation — 5 October 2026 (Mac)

This is a dated record of that Mac environment, not the current setup procedure or proof that the latest code is production-ready. Results combined inspected terminal screenshots with the tester's explicit confirmations; the assistant did not operate the Mac directly. The 7 October documentation review did not rerun these historical checks.

That environment created `.env.server` from its working `.env` without overwriting an existing file. An invalid Base64 JWT secret had caused `Illegal base64 character 5f` before it was corrected. These observations do not require other environments to copy those files or change their secrets.

| Check | Result on 5 October | Historical evidence |
| --- | --- | --- |
| Server Compose configuration | PASS | `config --quiet` completed without errors |
| Frontend and backend image build | PASS | Local server environment started successfully |
| Three services running | PASS | 20:29 screenshot showed all services up |
| Database healthcheck | PASS | 20:29 screenshot showed healthy |
| Frontend healthcheck | PASS | 20:29 screenshot showed healthy |
| Nginx `/healthz` | PASS | 20:29 screenshot showed HTTP 200 and `ok` |
| Registration and login | PASS | Tester confirmed new server environment login |
| Study and Feed save/reopen | PASS | Tester confirmed local server validation |
| Uploaded image display after refresh | PASS | Tester confirmed local server validation |
| Data persistence after `down` / `up` without `-v` | PASS | Tester confirmed account, Study, Feed and image retention |
| Secret file excluded from Git | PASS | 20:27 screenshot showed `.env.server` ignored |
| Deployment configuration/template visible to Git | PASS | 20:27 screenshot listed the new files |
| Full browser regression after switching to `.env.server` | PENDING | That record showed service checks only |
| Shared-server access and HTTPS | NOT RUN | No shared server was provisioned |
| Real Turnstile protection | NOT RUN | Local testing used always-pass keys |
| Backup and restoration | NOT RUN | No restoration exercise was performed |
| Automated CD | NOT IMPLEMENTED | No deployment workflow was created by that work |

No aggregate functional test count is claimed by this historical record. Confirm current behavior against the intended revision and environment before any shared-server rollout.
