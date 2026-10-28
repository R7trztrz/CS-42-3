# SurveyPlatform Backend

Backend service for SurveyPlatform, a University of Sydney COMP5703 project. Built with Java, Spring Boot, and PostgreSQL, it provides the foundation for developing the survey platform.

The backend currently includes researcher authentication and authorization, Study and feed-template APIs, the reusable question bank, versioned questionnaire-draft APIs with FR38 single-choice and scale branching, and immutable questionnaire publication snapshots.

Implemented functionality includes researcher registration and login, BCrypt password hashing, JWT issuance and validation, stateless request authentication, RESEARCHER role-based authorization, authenticated researcher identity handling, password change, Cloudflare Turnstile verification for registration, and rate limiting for authentication endpoints.

The backend also exposes OpenAPI documentation through Swagger UI. Flyway owns the PostgreSQL schema, and Spring Security protects researcher endpoints with JWT bearer authentication.

## Technology Stack and Dependencies

| Component | Version / Configuration | Purpose |
| --- | --- | --- |
| Java | 17 | Compilation target and recommended development JDK |
| Spring Boot | 4.1.1 | Application configuration, dependency management, and startup |
| Maven Wrapper | Maven 3.9.16 | Consistent build tooling without a separate Maven installation |
| Spring Web MVC | Managed by Spring Boot | REST APIs, JSON conversion, and an embedded web server |
| Spring Data JPA / Hibernate | Managed by Spring Boot | Entity mapping, database access, and transaction support |
| PostgreSQL JDBC | Managed by Spring Boot | PostgreSQL database connectivity |
| Flyway + PostgreSQL module | Managed by Spring Boot | Database migration management and execution |
| Spring Security OAuth2 Resource Server | Managed by Spring Boot | Stateless Bearer-token authentication, JWT validation, and role-based authorization |
| Validation | Managed by Spring Boot | Input validation using Jakarta Bean Validation |
| springdoc OpenAPI | 3.1.0 | OpenAPI documentation generation and Swagger UI |
| Apache Commons CSV | 1.14.1 | CSV parsing, writing, and field escaping |
| Actuator | Managed by Spring Boot | Health checks and application management endpoints |
| Lombok | Managed by Spring Boot | Compile-time generation of constructors, accessors, and other boilerplate |
| DevTools | Managed by Spring Boot | Automatic application restarts after recompilation during development |
| Spring Boot Test Starters | Managed by Spring Boot | Test support for MVC, JPA, security, validation, Flyway, and Actuator |
| Testcontainers PostgreSQL | Managed by Spring Boot | Repeatable PostgreSQL integration tests using the production database engine |
| BCrypt | Via Spring Security | Researcher password hashing and verification |
| Cloudflare Turnstile | External service | Human verification for researcher registration |
| Bucket4j | 8.20.0 | In-memory rate limiting for authentication endpoints |

Refer to `pom.xml` for the dependency definitions.

## Project Structure and Architecture

```text
SurveyPlatformBackend/
├── .mvn/wrapper/                  # Maven Wrapper configuration
├── src/main/java/com/cs_42_3/surveyplatformbackend/
│   ├── common/
│   │   └── exception/             # Shared API error handling
│   ├── config/                    # Security and application configuration
│   ├── researcher/
│   │   ├── api/                   # Researcher authentication APIs
│   │   ├── domain/                # Researcher entity and roles
│   │   ├── repository/            # Researcher persistence
│   │   └── service/               # Authentication and JWT services
│   ├── security/
│   │   ├── ratelimit/             # Authentication rate limiting
│   │   └── turnstile/             # Cloudflare Turnstile verification
│   ├── study/                     # Study APIs, domain and services
│   ├── survey/                    # M4 question bank and questionnaire APIs
│   └── SurveyPlatformBackendApplication.java
├── src/main/resources/
│   ├── application.yaml           # Application configuration
│   └── db/migration/              # Flyway database migrations
├── src/test/java/com/cs_42_3/surveyplatformbackend/
│   ├── survey/                    # Unit, MVC, repository, and concurrency tests
│   └── TestcontainersConfiguration.java
├── .env.example                   # Sanitized environment template
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

The project uses Flyway for database schema management. Migration scripts are stored under `src/main/resources/db/migration/`.

The current migration sequence is:

- V1-V5: Study, researcher, runtime-setting, and feed-template foundations
- V6: seeded Facebook feed template
- V7: Study publication token and publication timestamp
- V8: synchronized social-feed template documents
- V9: Study image assets and storage metadata
- V10: main's immutable synchronization of all seven platform feed templates
- V11: reusable question bank and stable question-option identities
- V12: versioned questionnaire drafts and stable ordered item identities
- V13: deterministic questionnaire branch rules
- V14: immutable, self-contained questionnaire publication snapshots
- V15: participant sessions, consent metadata, and lifecycle state
- V16: immutable answer paths and persisted participant answers
- V17: study closure timestamps, preserving legacy closed records
- V18: durable asset file cleanup tasks independent of deleted studies
- V19: eye-tracking calibration attempts, low-quality reasons, point residuals and the attempt each session continued with (FR-49)
- V20: gaze batches and gaze events, plus the per-session eye-tracking quality view (FR-50, FR-51)

Legacy feature databases with V10 question-bank / V16 template-sync history
must follow [the upgrade guide](../M4_M5_Migration_Upgrade_Guide.md), not an
in-place restart with renamed migrations. Main V1-V10 is preserved byte-for-byte.

The current setting, `spring.jpa.hibernate.ddl-auto=validate`, instructs Hibernate to validate entity mappings against the Flyway-managed database schema without automatically creating or modifying database tables. Add future migrations using the next immutable version number.

## Authentication and Security

Researcher authentication is implemented using Spring Security and JWT.

Current authentication and security features include:

- Researcher registration with email and password validation
- BCrypt password hashing and verification
- Researcher login with generic invalid-credential responses
- Signed JWT access tokens
- Stateless JWT validation for protected requests
- `RESEARCHER` role-based authorization
- Authenticated researcher identity extraction for resource ownership
- Researcher password change with current-password verification
- Cloudflare Turnstile verification during registration
- Per-IP rate limiting for registration and login

Public authentication endpoints:

- `POST /auth/register`
- `POST /auth/login`

Authenticated endpoint:

- `POST /auth/change-password`

Swagger UI is available while the backend is running at:

`http://localhost:8080/swagger-ui/index.html`

### JWT Configuration and Token Generation

The backend uses an HS256 JWT signing key. `JWT_SECRET` must contain a Base64-encoded random secret.

A suitable development secret can be generated with:

```bash
openssl rand -base64 32
```

Copy the generated value into the backend `.env` file:

```env
JWT_SECRET=<generated-base64-secret>
```

Do not commit the generated secret to the repository.

JWT access tokens are issued automatically after a successful researcher login.

Send a request to:

```text
POST /auth/login
```

with a valid researcher email and password. A successful response contains the JWT access token.

The token should be included in protected API requests using the HTTP `Authorization` header:

```text
Authorization: Bearer <access-token>
```

When using Swagger UI, call `/auth/login`, copy the returned access token, click **Authorize**, and paste the token into the Bearer authentication field.

## Prerequisites

1. Install JDK 17, set `JAVA_HOME` to the JDK installation directory, and add its `bin` directory to `PATH`.
2. Create a PostgreSQL database and ensure it is accessible. The database account must have the permissions required for application reads and writes and migration execution.
3. Ensure network access is available on the first Maven Wrapper run to download Maven and project dependencies.
4. Install and start a Docker-compatible container runtime to run PostgreSQL integration tests. Container-backed tests intentionally fail when Docker is unavailable so a required database gate cannot be reported as skipped.

All database values in this document are examples. Replace them with the values for your environment. Do not store actual passwords in this README, `application.yaml`, or shared run configurations.

| Environment Variable | Example Value | Description |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/example_database` | JDBC URL containing the host, port, and database name |
| `SPRING_DATASOURCE_USERNAME` | `example_user` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `replace-with-local-password` | Database password |
| `JWT_SECRET` | `replace-with-base64-encoded-secret` | Base64-encoded secret used to sign and verify JWT access tokens |
| `TURNSTILE_SECRET` | `your-turnstile-secret` | Cloudflare Turnstile server-side verification secret |
| `SPRING_PROFILES_ACTIVE` | `dev` | Explicit development-only M5/M6 adapter wiring; not for production |

`application.yaml` reads these values from environment variables. The datasource variables configure the PostgreSQL connection, while `JWT_SECRET` and `TURNSTILE_SECRET` are security secrets used by the application. These values must be kept separate from platform user credentials and must not be committed to the repository.

Copy `.env.example` to a local `.env` file to store your configuration. Spring Boot and Maven do not load `.env` files automatically.

For local development, explicitly select the `dev` profile. M5 requires a
`CollectionCompletionGate`; this repository supplies only a `dev`/`test` no-op
until the real M6 adapter is connected. Without one of these profiles or a real
adapter, startup deliberately fails rather than silently skipping production
collection checks. Do not enable `dev` on a real participant research deployment.
See [the deployment guide](../Server_Deployment_Guide.md) for server-style demos.

## Running with IntelliJ IDEA

1. Import the backend directory or its `pom.xml` as a Maven project. Select JDK 17 and the project Maven Wrapper.
2. Copy `.env.example` to `.env` in the backend directory and configure the database, JWT secret, and Turnstile secret values.
3. Open **Run → Edit Configurations** and select or create the run configuration for `SurveyPlatformBackendApplication`.
4. In **Environment variables**, use **Browse for .env files and scripts** to select the backend `.env` file. If the field is hidden, enable it through **Modify options → Environment variables**.
5. For development, set **Active profiles** to `dev` (or load `SPRING_PROFILES_ACTIVE=dev` from the backend `.env`); merely creating the file is not enough.
6. Apply the configuration and run the application. Do not use both Docker and IntelliJ backends on the same host port.

For details, see the [IntelliJ IDEA documentation on environment variables and .env files](https://www.jetbrains.com/help/idea/program-arguments-and-environment-variables.html#environment-variables).

Keep `.env` local and excluded from version control. The application run configuration does not configure independent PowerShell sessions or test runs; configure the test run to load `.env` as well when needed.

## Running with PowerShell

Open PowerShell in the repository root and navigate to the backend directory:

```powershell
Set-Location .\SurveyPlatformBackend
java -version
.\mvnw.cmd -v
```

Set each environment variable, then start the application:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/example_database'
$env:SPRING_DATASOURCE_USERNAME = 'example_user'
$env:SPRING_DATASOURCE_PASSWORD = 'replace-with-local-password'
$env:JWT_SECRET = 'replace-with-a-base64-encoded-secret'
$env:TURNSTILE_SECRET = 'your-turnstile-secret'
$env:SPRING_PROFILES_ACTIVE = 'dev'

.\mvnw.cmd spring-boot:run
```
> The actual Cloudflare Turnstile secret must not be committed to the repository.
> Team members should obtain the development secret through a private channel and store it only in their local `.env` file.

These variables apply only to the current PowerShell session and its child processes. They remain available when restarting the application in the same session, but must be set again in a new session. The `dev` value is only for local development, not production. Press `Ctrl + C` to stop the application.

Entering a password directly in a command may save it in the terminal history. To enter the password through a credential prompt instead, replace the password assignment above with:

```powershell
$credential = Get-Credential -UserName $env:SPRING_DATASOURCE_USERNAME -Message 'Enter the database password'
$env:SPRING_DATASOURCE_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
```

## Building and Testing

Use the repository Maven Wrapper so Windows, Linux, and macOS use Maven 3.9.16 consistently.

Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Linux or macOS:

```bash
./mvnw test
```

With Docker available, the full command starts isolated PostgreSQL containers, applies Flyway V1 through V20, and runs repository, publication, migration-upgrade, and concurrency integration tests. Machine-dependent timing checks are opt-in:

```powershell
.\mvnw.cmd -Pperformance test
```

## M4 question bank and questionnaires

M4 is contained under the top-level `survey` package. The frozen frontend
integration contract is distributed separately as `m4-api-contract-v1.md`;
it includes request and response examples, the error envelope, branch
semantics, and migration notes.

Question-bank APIs are under `/api/questions`. The list endpoint returns the
stable `QuestionPageResponse` envelope and accepts `page`, `size`, `type`, and
`search`; create, detail, update, and delete use `questionId`. Choice-option
identities are generated by the server and returned as `optionId`. Updates must
retain existing option IDs, unless `replaceAllOptions: true` explicitly replaces
every option identity.

Questionnaire composition remains a `survey.questionnaire` subdomain and exposes:

```text
GET /api/studies/{studyId}/questionnaire
PUT /api/studies/{studyId}/questionnaire
```

`GET` returns live question-bank data while the owned Study is `DRAFT`; after
publication it returns the immutable snapshot with
`contentSource: PUBLISHED_SNAPSHOT`. A missing draft questionnaire returns HTTP
404 `QUESTIONNAIRE_NOT_FOUND`, which an editor may interpret as its unsaved state.

`PUT` submits the complete desired item order. Existing `itemId` values must be
retained when reordering or replacing items. The `expectedVersion` JSON property
is mandatory: a first save uses `null`, and subsequent changes submit the version
returned by `GET`. Identical retries are idempotent and do not advance the
questionnaire version. Only `DRAFT` studies are editable.

FR38 rules are supplied on each source item. `SINGLE_CHOICE` uses
`sourceOptionId`; `SCALE` uses `sourceScaleValue`; exactly one trigger must be
present. `targetPosition` is a zero-based index into the final submitted item
array:

```json
{
  "expectedVersion": 3,
  "items": [
    {
      "itemId": "11111111-1111-1111-1111-111111111111",
      "questionId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "branchRules": [
        {
          "sourceOptionId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
          "sourceScaleValue": null,
          "targetPosition": 1
        }
      ]
    },
    {
      "itemId": "22222222-2222-2222-2222-222222222222",
      "questionId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
      "branchRules": []
    }
  ]
}
```

When no explicit rule matches, the flow continues to the next item; the last
default transition reaches END. Optional branching questions always retain the
no-answer default path. Saves reject duplicate triggers, invalid option or scale
values, invalid targets, cycles, unreachable items, and non-terminating items.
Only DRAFT studies are editable.

Question updates retain explicitly submitted option IDs. Draft questionnaires
read the latest live question data. Deleting a question clears its outgoing rules,
retains incoming references to the now-missing item, and makes that draft visibly
invalid until repaired. `GET` returns stable item and target IDs, per-item
reference status, `valid`, and structured `validationIssues`.

When a Study with `questionnaireEnabled: true` is published, publication validates
that the questionnaire exists, is nonempty, contains no missing questions, and
has a terminating, acyclic, reachable flow. It then creates one immutable,
self-contained snapshot in the same transaction that changes the Study to
`COLLECTING`. Later question-bank edits or deletions do not change published
content. The M5 runtime uses the stateless snapshot branch resolver; participant
sessions and answer persistence live in the separate `participation` module.

## M5 anonymous sessions and questionnaire execution

Anonymous study entry uses `GET /api/participation/{token}`. Starting a study
uses `POST /api/participation/{token}/sessions` and returns a session token once.
Session operations use `X-Participant-Session-Token`, not a researcher JWT or
the public study-link token. Core routes under `/api/participant-session` are:

- `GET /api/participant-session`: restore the server-saved phase and progress.
- `PUT /api/participant-session/consent`: accept or decline consent.
- `POST /api/participant-session/browsing-completion`: finish browsing.
- `GET /api/participant-session/questionnaire/current`: read the current item.
- `PUT /api/participant-session/questionnaire/answers/{itemId}`: submit an answer
  with a UUID `Idempotency-Key`; the server determines the next item.
- `POST /api/participant-session/questionnaire/submission`: complete the path.
- `POST /api/participant-session/abandonment`: explicitly exit.

Answers and actually visited steps are persisted independently of the live
question bank. Exact retries do not create duplicates; completed/abandoned
sessions and closed studies reject further writes. Non-eye-tracking demo
sessions can move from consent to browsing and questionnaire completion.
Eye-tracking calibration, real M6 collection completion, and approved consent
remain integration prerequisites for actual participant research.

## Creating a study with a feed template

`POST /api/studies` now requires a template identifier:

```json
{
  "title": "Social media study",
  "description": "Example description",
  "templateCode": "blank"
}
```

Supported codes are `blank`, `facebook`, `instagram`, `tiktok`, `x`,
`threads`, `bluesky`, and `truth-social`. Missing codes return HTTP 400;
unknown codes return HTTP 400 with `FEED_TEMPLATE_NOT_FOUND`.

Flyway V5 creates the read-only application catalog `feed_templates` and the
per-study `study_feeds` table. The stable template code is its primary identifier.
All eight initial template documents and schema versions are SQL NULL until the
frontend supplies their content. The blank template is also a placeholder, not
an implemented empty editor document.

Creation atomically saves the study and an independent copy of its template's
content, theme and schema version. A placeholder can create a study, but its null
content must not be treated as publishable. Template changes affect future
creations only; existing copies are not overwritten. Existing studies are not
backfilled because their original template choice is unknown.

Add template content through a new migration after agreeing on the editor JSON
contract. Do not rewrite V5 after it has been applied. Legacy-study initialization
remains separate follow-up work.

## Study publication (FR-14)

Flyway V7 adds a unique participation token and publication timestamp.
Send a researcher JWT to `POST /api/studies/{studyId}/publish` with the current
**study** version, not the feed version:

```json
{
  "version": 0
}
```

Only an owned DRAFT with a nonempty feed JSON object can be published.
Publication atomically assigns a random participation token, records publishedAt,
changes status to COLLECTING and updates the study timestamp and JPA version.
The 200 response contains study details, the refreshed version, publishedAt and
participationUrl. The owner's detail endpoint also returns this URL later.
COLLECTING and CLOSED cannot be republished. Study and feed editing are blocked
after publication. Publication and feed saves lock the same study row.

When `questionnaireEnabled` is true, publication validates the complete M4
questionnaire and atomically creates its immutable snapshot. Missing, empty, or
invalid questionnaires return HTTP 409
`QUESTIONNAIRE_PUBLICATION_VALIDATION_ERROR` with structured `details`. When the
switch is false, no questionnaire or snapshot is required. Detailed Craft.js feed
validation remains separate from questionnaire readiness.

Configure the frontend base URL (defaults to the local frontend origin):

```powershell
$env:APP_PARTICIPANT_BASE_URL = "http://localhost:5173"
```

The backend appends `/participate/{token}`. Set the deployed frontend base URL
in production. The frontend must implement this page separately.

`GET /api/participation/{token}` requires no researcher JWT. It returns title,
description, runtime switches, feed theme and JSON content, without management
metadata. Public entry/asset reads and anonymous session creation are permitted;
subsequent participant operations require the independent session token. Responses use
`Cache-Control: no-store`; every read checks the current study state.
Invalid links return 404 PARTICIPATION_NOT_FOUND; CLOSED returns 410 STUDY_CLOSED.

Publishing returns 404 STUDY_NOT_FOUND for missing or foreign-owned studies, or
409 STUDY_NOT_PUBLISHABLE, STUDY_VERSION_CONFLICT or FEED_NOT_READY.
Invalid versions return 400; framework error normalization is unchanged.

M5 session/questionnaire execution is implemented separately as described above;
FR-16 closure uses the separate close endpoint documented below. Questionnaire
saves use the study lock and DRAFT-only rule, and participant writes recheck state.

Run the focused suite (isolated H2 storage and production security filter rules):

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyPublicationApiTest,StudyApiTest,StudyPersistenceTest,FeedApiTest" test
```

Live PostgreSQL/Flyway and frontend participation-page integration require
separate verification.

### FR-16: permanently close a study

Send `POST /api/studies/{studyId}/close` with a researcher Bearer JWT and
`Content-Type: application/json`. The request must carry the numeric version
returned by the study details endpoint:

```json
{"version": 2}
```

Only the owner may close a COLLECTING study. Closure returns HTTP 200 with the
current `StudyResponse`, including `status: "CLOSED"`, `closedAt`, `updatedAt`
and the incremented `version`. The transition is irreversible. A missing or
foreign-owned study returns 404 `STUDY_NOT_FOUND`; a draft returns 409
`STUDY_NOT_CLOSABLE`; a stale version on a collecting study returns 409
`STUDY_VERSION_CONFLICT`. Missing, string, negative, fractional or out-of-range
versions return 400. Errors use the existing global `{code, error}` contract.

Retrying closure of an already CLOSED, owned study returns its current details
without changing timestamps, versions or sessions. A syntactically valid stale
version is accepted for that retry. Legacy CLOSED rows may have a null
`closedAt`; migration V17 does not invent historical timestamps.

The command holds the Study write lock and invokes
`ParticipantSessionLifecyclePort.abandonActiveSessionsForStudy` in the same
transaction. Active sessions become ABANDONED with reason STUDY_CLOSED;
completed and previously abandoned sessions remain unchanged. Existing answers,
device information, published questionnaire snapshots, feed content and image
assets are retained. Any failure rolls back both the study and session updates.

Participant writes use the same Study-before-session lock order. An accepted
write holding the shared Study lock commits before closure; a write waiting
behind closure observes CLOSED and is rejected. The participation token and URL
are retained for a clear 410 STUDY_CLOSED response from participant entry and
content endpoints. The owner can still query study details. Existing pages are
not forcibly disconnected; their subsequent participation requests are rejected.

Future behavior/gaze persistence must call `ParticipantCollectionPolicy` in the
same transaction as each batch insert, including retries and buffered uploads.
The presence of this port does not mean those collectors are implemented.
Closure does not add participant statistics, data exports or frontend controls.
Frontend consumers should confirm the irreversible action before calling and
refresh their study state after success.

Focused verification:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyClosureServiceImplTest,StudyClosureControllerTest,StudyClosurePersistenceTest,StudyClosurePostgresIntegrationTest" test
```

The first three suites run without Docker; the persistence suite uses isolated
H2 transactions and the real M5 bulk update. The PostgreSQL suite applies the
Flyway migrations and tests both closure/answer concurrency orders using actual
database lock waits. It requires Docker and is explicitly skipped when Docker
is unavailable; H2 results do not establish PostgreSQL lock behavior.

### FR-18: permanently delete a study

Send `DELETE /api/studies/{studyId}?version=2` with a researcher Bearer JWT.
Use the current study `version` returned by the details endpoint. No request
body is required. The frontend must obtain explicit confirmation before sending
this irreversible command; this backend change does not add frontend controls.

The owner may delete a DRAFT, COLLECTING or CLOSED study. Success returns HTTP
204 with no body. Missing, already deleted and foreign-owned studies return
404 `STUDY_NOT_FOUND`; a stale version returns 409 `STUDY_VERSION_CONFLICT`.
Missing, negative, fractional and out-of-range versions return 400. Errors use
the existing global `{code, error}` response.

The transaction locks the Study before deleting its feed, image metadata,
questionnaire, branch rules, publication snapshot, participant sessions, steps
and answers. Researcher accounts, reusable question-bank questions/options,
global feed templates and other studies remain unchanged. Old participant links
and sessions can no longer resolve the deleted study. An unexpected restrictive
foreign key fails the transaction rather than leaving partially deleted data.

Image storage keys are copied into `asset_file_cleanup_tasks` in the same
transaction. These tasks have no foreign key to studies or assets. No files are
deleted before commit; rollback preserves both database records and files.
After commit, image metadata is absent so business endpoints cannot serve the
images, even while physical cleanup is pending.

The cleanup scheduler polls committed tasks every 30 seconds and processes up
to 50 tasks per scan. Each task uses a separate transaction with
`FOR UPDATE SKIP LOCKED`, preventing concurrent workers from claiming it.
Successful deletion removes the task; an already absent file also succeeds.
Failure retains the task with an attempt count, a safe error description and
exponential retry delays from 30 seconds up to one hour. Tasks survive restarts.
Application logs identify failed task IDs. A crash after file deletion but before
task removal is safe because the next attempt accepts a missing file.

Optional environment variables:

```powershell
$env:ASSET_CLEANUP_SCHEDULER_ENABLED = "true"
$env:ASSET_CLEANUP_SCAN_INTERVAL = "30s"
```

Keep `ASSET_STORAGE_DIRECTORY` on persistent storage. If multiple backend
instances share the database, their workers must access the same asset storage;
independent local directories are not suitable for distributed cleanup.

Future behavior/gaze persistence must check `ParticipantCollectionPolicy` in
the same transaction as inserts and retain the Study-before-session lock order.
Study-owned collection tables must use cascading foreign keys (normally through
the participant session) or be explicitly included in `StudyDeletionRepository`.
A missing study/session must reject writes, including delayed uploads and retries.
Those collectors are not implemented by this deletion feature.

Focused verification:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyDeletionControllerTest,StudyDeletionPersistenceTest,StudyDeletionPostgresIntegrationTest" test
```

The H2 suite checks real JPA/JDBC rollback, dependency deletion and isolation,
committed-task visibility, file removal and persisted retries. It uses a reduced
FK fixture and the actual V18 migration. The Docker-based PostgreSQL suite checks
the actual migration history, deletion of published questionnaire data and both
answer/deletion concurrency orders. It is skipped when Docker is unavailable.

### FR-19: participation overview and anonymous progress list

Send the following request with a researcher Bearer JWT:

```http
GET /api/studies/{studyId}/participation?page=0&size=20&sortBy=enteredAt&direction=desc
```

The owner can query a DRAFT, COLLECTING or CLOSED study. Missing and foreign-owned
studies return 404 `STUDY_NOT_FOUND`. The response uses `Cache-Control: no-store`
and contains `summary` and `sessions`:

```json
{
  "summary": {"total": 0, "inProgress": 0, "completed": 0, "abandoned": 0},
  "sessions": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

The example shows a study with no sessions. Each actual list entry contains only `sessionId`,
`anonymousParticipantId`, `status`, `enteredAt` and `completedAt`. Use `sessionId`
for the subsequent session-details feature. Completion time is null for sessions
that have not completed. Device payloads, session tokens/hashes, answers and raw
behavior/gaze data are never returned by this endpoint.

Counts describe persisted sessions, including declined-consent sessions, rather
than link clicks or distinct human participants. Re-entering with a new session
can increase the count. `total` equals `inProgress + completed + abandoned`.
An owned study with no sessions returns zero counts and an empty page. This query
does not infer timeouts, mutate M5 status, or update study timestamps.

Parameters default to `page=0`, `size=20`, `sortBy=enteredAt`, `direction=desc`.
Pages are zero-based; size must be between 1 and 100. The page offset must fit a
32-bit integer. Only `enteredAt` and `status` are accepted sort fields, and only
lowercase `asc`/`desc` are accepted directions. Invalid values return 400 through
the existing global error handler.

Status ascending order is `IN_PROGRESS`, `COMPLETED`, `ABANDONED`; descending
reverses that order. Status ties use entry time descending, then session UUID
ascending. Entry-time sorting also uses session UUID ascending to break ties.
Sorting and pagination execute in the database; only the requested page is
loaded. Existing V15 indexes remain unchanged pending PostgreSQL query-plan
evidence for additional indexes.

Ownership, counts and the page are read in one read-only REPEATABLE READ
transaction without explicit write locks. Concurrent writes appear on the next
request, keeping counts and rows consistent within one response. Separate page
requests may observe newly created or updated sessions.

Focused verification:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyParticipationControllerTest,StudyParticipationPersistenceTest,StudyParticipationPostgresIntegrationTest" test
```

The controller and H2 suites run without Docker. The PostgreSQL suite uses the
actual Flyway schema and checks snapshot consistency while another transaction
updates a session and inserts a new one. It is skipped without Docker. This
feature adds no frontend page, session-details endpoint or export pipeline.

### FR-20: participant session details

Use a session ID returned by FR-19 and authenticate with a researcher Bearer JWT:

```http
GET /api/studies/{studyId}/participants/{sessionId}
```

Only the study owner can read the session. Missing and foreign-owned studies
return 404 `STUDY_NOT_FOUND`; missing sessions and sessions outside the specified
study return 404 `PARTICIPANT_SESSION_NOT_FOUND`. Invalid UUIDs return 400;
authentication and role failures return 401/403. CLOSED studies remain readable.
Deleting the study makes its session details unavailable.

The response includes:

- `studyId`, `sessionId` and `anonymousParticipantId`;
- `status`, `phase` and `abandonmentReason`;
- `enteredAt`, `consentedAt`, `calibrationCompletedAt`, `browsingCompletedAt`,
  `completedAt`, `abandonedAt` and `lastActivityAt`;
- `deviceInfo`, restricted to `browser`, `browserVersion`, `os`, `screenWidth`,
  `screenHeight` and `timezone`;
- `eyeTracking`, containing `enabled`, `calibrationState`,
  `calibrationAttemptCount`, `completedAttemptId`, `trackingAvailable`,
  `unavailableReason`, `quality`, `qualityBasis` and `lowQualityReasons`.

Missing timestamps stay null. Device information is null when not supplied or
cleared after consent refusal. Unknown device JSON keys are ignored rather than
passed through. No questionnaire answers, behavior/gaze events, calibration
point measurements, session credentials or download URLs are returned.

The eye-tracking module reads the existing `eye_tracking_session_quality` view
from V20. The final grade and availability belong to the attempt referenced by
`calibration_completions`, not the most recently uploaded attempt. No score is
recalculated, and provisional attempts are not substituted for a missing final
record. The summary distinguishes these states:

| `calibrationState` | Meaning |
|---|---|
| `DISABLED` | Eye tracking is disabled for the study. |
| `NOT_STARTED` | Eye tracking is enabled, but no attempt has been stored. |
| `IN_PROGRESS` | Attempts exist, but no final attempt has been selected. |
| `RECORDED` | A final attempt has been selected, including abandoned or unavailable outcomes. |

Without a final record, availability and quality fields remain null and
`lowQualityReasons` is empty. A recorded unavailable attempt instead returns
`trackingAvailable: false` and its stored reason. Available tracking is not proof
that gaze events were saved. The response intentionally contains no raw event
counts or claims that the captured data is complete.

The query uses one read-only REPEATABLE READ transaction for the JPA session
projection and M6 JDBC summary. It does not infer timeouts, transition session
states, update timestamps, acquire write locks or start an export. Responses use
`Cache-Control: no-store`. This feature introduces no database migration and no
frontend page; FR-21 and M7 remain separate work.

Focused verification:

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyParticipantDetailControllerTest,StudyParticipantDetailPersistenceTest,StudyParticipantDetailPostgresIntegrationTest,StudyParticipationControllerTest,StudyParticipationPersistenceTest" test
```

The H2 suite uses the actual V19 tables and a reduced, H2-compatible calibration
view. It verifies missing data, device allow-listing, session/study isolation,
read-only behavior and final-attempt selection. The PostgreSQL suite uses the
actual V20 view and checks the shared JPA/JDBC snapshot during concurrent
calibration completion. It is explicitly skipped without Docker.

## Session and study-wide data exports (M7)

All export endpoints require the owning researcher's Bearer JWT and the
`RESEARCHER` role. The frontend export UI is not included in this increment.

1. Create a task with
   `POST /api/studies/{studyId}/participants/{sessionId}/exports`:

   ```json
   { "dataTypes": ["QUESTIONNAIRE"] }
   ```

   To export every session in the study, send the same body to
   `POST /api/studies/{studyId}/exports` instead. All persisted session statuses
   are included, without state/time filtering. Study lifecycle state does not
   restrict reads; an empty study can also be exported.

   The response is `202 Accepted` with task metadata and a `Location` polling URI.
   Select any nonempty combination of `QUESTIONNAIRE`, `GAZE` and `BEHAVIOR`. Other categories are rejected. Sessions
   may be in progress, completed or abandoned, including sessions in closed studies.
2. Poll `GET /api/studies/{studyId}/exports/{exportId}`. States are `PENDING`,
   `RUNNING`, `COMPLETED`, `FAILED` and `EXPIRED`. Failed tasks include a safe
   error code/message; retry by creating a new task. `snapshotAt` records the
   generation read transaction start, not the original task request time.
   Metadata includes `scope` (`SESSION` or `STUDY`), `sessionId` (null for a study
   export), and `failedSessionId` when a session-specific failure occurs. Task
   states are pollable; this version does not expose a live percentage.
3. Download `GET /api/studies/{studyId}/exports/{exportId}/download` after completion.
   Supply the JWT on the download request as well. Responses use `no-store` and
   an attachment filename. Downloads return 409 before completion, 410 after
   expiry/file loss and 404 for missing/foreign tasks or deleted studies.

For gaze-only exports, use `{"dataTypes":["GAZE"]}`. For a combined export, use
`{"dataTypes":["QUESTIONNAIRE","GAZE","BEHAVIOR"]}`. For behavior only, use
`{"dataTypes":["BEHAVIOR"]}`. All seven nonempty combinations are supported.
The selected categories are persisted
with the task and returned unchanged by polling. Each writer runs only when its
category was selected: behavior-only exports do not read or validate questionnaire or raw gaze records.

Each UTF-8 ZIP contains `sessions.csv`, `README.md` with schema v4, and the
selected category files below when records exist. Existing schema-v1/v2/v3 archives
remain downloadable until their original expiry. Session and response CSV
columns are unchanged from v1.

| File | Contents |
|---|---|
| `responses.csv` | Saved questionnaire steps and answers, when `QUESTIONNAIRE` is selected. |
| `gaze_events.csv` | One row per saved gaze prediction, when `GAZE` is selected. |
| `gaze_batches.csv` | Original received batch metadata and browser-reported dropped counts. |
| `calibration_attempts.csv` | All stored attempts, including unavailable/abandoned outcomes and the final-selection flag. |
| `behavior_events.csv` | Raw behavior payloads and readable event-specific columns, when `BEHAVIOR` is selected. |
| `behavior_batches.csv` | Stored stream/batch identities, sequence, version, event count and receipt time. |
| `data_availability.csv` | Study scope only: per-session file availability, row counts and session notes. |

Questions and option
labels come from the immutable publication snapshot, never the current question
bank. One response row represents one saved step: `ANSWERED` includes the raw
answer JSON and readable value, while `SKIPPED` represents an optional unanswered
step. Unvisited branches and not-yet-submitted questions do not create rows.
Multi-choice readable values are JSON string arrays. If the questionnaire was
disabled or has no saved submissions, the response file is omitted and the reason
is recorded in README. Partial sessions are legitimate, not validation failures.
Session metadata includes stored lifecycle timestamps and final calibration
quality metadata; it includes no credentials, device payloads or raw gaze coordinates.

Study-wide files combine records from all sessions using the exact same per-session
row writers and validation. They contain one header per CSV and are grouped by
session UUID ascending, with each session retaining the ordering described below.
Missing data for one session does not create placeholder data rows. A data file is
omitted only if it has no rows across the entire study or its category was not
selected. An empty study produces just `sessions.csv` headers and README.

For studies with sessions, `data_availability.csv` has one row for each session
and potential category file. Columns are `session_id`, `file_name`, `status`,
`reason`, `record_count` and `session_notes`. Status is `PRESENT`, `NO_SAVED_RECORDS`
or `NOT_SELECTED`. Known disabled switches are recorded as `QUESTIONNAIRE_DISABLED`
or `EYE_TRACKING_DISABLED`; otherwise absence is reported without guessing its
cause. `session_notes` contains the selected-category notes and warnings once on
that session's first manifest row, and applies to the whole session. Counts are
export receipt metadata, not scientific metrics. The manifest does not claim
that unreceived browser data was captured or lost. No feed/image/configuration
assets are bundled by a study data export.

Gaze events retain `x/y` in viewport CSS pixels, scroll offsets, viewport size,
device ratio, page ID, widget attribution, browser wall-clock timestamp and
`session_elapsed_ms`. Values are not rounded, clamped or filtered by grade or
widget attribution. Order is `session_elapsed_ms`, then `batch_sequence`, then
`event_index`; negative elapsed values and equal timestamps are valid. Each event's
quality/basis/reasons come from its own `calibration_attempt_id`, which may differ
from the final attempt described in `sessions.csv`. No heatmaps, fixation metrics
or new grades are computed. Attempt CSV includes stored thresholds, counts,
reported medians, viewport and timestamps; per-target residual rows are not
included in this increment. No camera images or video are exported.

Empty eye-tracking files are omitted with explanations in README. Calibration
attempts are still included when present even if no gaze predictions were saved.
Study-disabled, camera-unavailable and not-yet-started sessions are legitimate
empty-data cases. A usable calibration does not prove predictions were delivered.
Batch sequence gaps and browser-reported dropped events produce README warnings
while saved predictions remain available. Cumulative dropped counts must not be
summed across batches; reloads can reset browser counters. There is no persisted
final expected sequence, so lost tail batches and wholly lost streams cannot be
detected from these tables. An export is evidence of stored records, not a claim
of complete browser delivery.

Behavior CSV includes click, keydown, mousemove, scroll and visibilitychange
records that actually exist. Common columns identify the study, session, stream,
batch, sequence, array index, type, page, component and client event time.
Expanded columns `x`, `y`, `key_code`, `scroll_x`, `scroll_y`, `hidden` extract the
stored `payload` JSON; fields that do not apply remain blank rather than zero or
false. The full stored JSON is retained in `payload`. No typing speed, click
counts or other scientific metrics are derived. Rows are ordered by
`session_elapsed_ms`, `batch_id`, `event_index`, matching the existing V23 index.

Batch CSV is ordered by `stream_id`, `sequence`, `batch_id`. Sequence gaps are
counted separately for each stream; a new stream starting at 1 is valid. Missing
sequences produce warnings while stored events remain exportable. No behavior
drop counter or final expected sequence exists, so delivery completeness and
lost tail batches cannot be established. Empty behavior files are omitted with
an explanation; absent types do not distinguish unimplemented collection, no
activity or lost uploads. Clock-origin changes after a reload are not repaired
by sorting. Event validation reuses the upload rules per row, without applying
today's upload batch-size limits to historical records.

Generation uses a read-only REPEATABLE READ transaction and a JDBC cursor for
steps/answers and gaze rows. Invalid item references, answer types/options,
branch paths, submission ordering, batch/event count or index mismatches,
cross-session calibration references and non-finite gaze values fail the task
instead of publishing a partial archive. Browser wall-clock timestamps are not
compared to server timestamps because client clocks can differ.
CSV uses RFC 4180 escaping, UTC ISO-8601 timestamps and UTF-8 without BOM.
Research text is preserved, including formula-like prefixes: import columns as
text when opening raw exports in spreadsheet software.

Private files default to `./data/exports`, relative to the backend working
directory, and are ignored by Git. Optional environment variables:

```dotenv
# Persist outside the checkout in deployed environments.
EXPORT_STORAGE_DIRECTORY=./data/exports
# Download validity starts after successful generation.
EXPORT_RETENTION=24h
# Keep enabled on an instance responsible for task generation and cleanup.
EXPORT_SCHEDULER_ENABLED=true
# Poll interval for task generation, expiry and file cleanup.
EXPORT_SCAN_INTERVAL=2s
```

V21 adds durable task and independent file-cleanup tables. V22 extends the
category constraint without changing V21 tasks and adds the gaze ordering index.
V24 extends the constraint to all three categories and their combinations,
retaining existing task selections and V23 behavior storage unchanged.
V25 adds scope, allows a null target session only for STUDY jobs, and records
safe failure-session identity. Existing jobs are backfilled as SESSION without
changing their data selections, timestamps, status or download expiry.
A dedicated scheduler
claims tasks using PostgreSQL row locks and `SKIP LOCKED`. A committed `RUNNING`
task without a live processing lock is recovered after restart; temporary files
are overwritten safely. The worker retains the task lock while a separate
transaction reads the source snapshot, so the connection pool needs at least
two connections per active worker (the normal Boot pool default is sufficient
for this single worker). For multiple instances, all workers must share the
same private export storage and database; otherwise run the worker on a single
instance and route downloads to storage-accessible instances.

Study generation uses keyset pages of 200 session IDs inside one generation-time
REPEATABLE READ snapshot; it does not open a transaction per session or collect
all study records in memory. At most eight temporary CSV writers append to files
under the private `<exportId>.work` directory. After all sessions validate, the
files are packed into one ZIP and the workspace is removed. A failure publishes
no archive; session-specific errors identify `failedSessionId`, and the durable
cleanup task removes the ZIP, `.part` file and flat workspace. Restart recovery
clears the job's old workspace before regenerating it. Long exports retain a
database snapshot and a task lock, so study deletion may wait for generation.
Collection writes continue without waiting on a Study read lock from the exporter.

Deleting a study waits for its active generators, queues both ZIP and temporary
file cleanup in the deletion transaction, then cascades task rows. File deletion
runs after commit, retries failures with bounded backoff and treats missing files
as success. Expired and failed artifacts use the same durable cleanup mechanism.
Already-open downloads may finish; new downloads after deletion/expiry are denied.

Focused verification:

```powershell
.\mvnw.cmd "-Dtest=StudyExportWriterTest,QuestionnaireExportWriterTest,GazeExportWriterTest,BehaviorExportWriterTest,ExportDataTypeTest,ExportServiceTest,ExportWorkerTest,LocalExportStorageTest,ExportControllerTest,ExportApplicationWiringTest,ExportPostgresIntegrationTest" test
```

The application-wiring test starts an isolated H2 context with migrations and schema
validation disabled; it verifies component injection and scheduler separation only.
The PostgreSQL suite uses actual Flyway migrations and is skipped without Docker.
Frontend export controls and end-to-end browser acceptance remain separate work;
backend checks alone do not establish delivery of every M7 user interface requirement.

## Participant behavior uploads (FR-47/48 backend)

Frontend developers can use the standalone [behavior upload integration guide](docs/behavior-upload-integration.md)
for the request contract, queue lifecycle, error handling and acceptance checklist.

`POST /api/participant-session/behavior-batches` accepts a JSON batch with the
existing `X-Participant-Session-Token` header. Use the opaque participant token,
not the researcher's JWT. In Swagger, authorize `participantSessionToken`.
The server derives the session and study from authentication; `sessionId` and
`studyId` are not accepted in the request body. No frontend collector/uploader
is added by the upload module. Persisted behavior can be downloaded through the
researcher-only `BEHAVIOR` export category documented above.
The body uses an `events` JSON array, not the gaze endpoint's serialized `payload`
string. Event count is derived by the server; do not send a separate `eventCount`.

```json
{
  "schemaVersion": 1,
  "batchId": "36db590d-2945-4bd3-a16a-640289316fcb",
  "streamId": "05520822-f473-4ad2-bf0c-a04d15df4880",
  "sequence": 1,
  "events": [
    {
      "type": "click",
      "timestamp": 1793145600000,
      "sessionElapsedMs": 1250.5,
      "pageId": "feed-page",
      "x": 320,
      "y": 180,
      "componentId": "post-1",
      "componentType": "PostWidget"
    },
    {
      "type": "keydown",
      "timestamp": 1793145600100,
      "sessionElapsedMs": 1350.5,
      "pageId": "feed-page",
      "code": "ArrowDown",
      "componentId": null,
      "componentType": null
    }
  ]
}
```

The server returns `201 Created` for a new committed batch or `200 OK` for an
identical replay. Both return the original acknowledgement shape:

```json
{
  "sessionId": "a60c8e68-2cd5-48d3-a0fa-84e21484a949",
  "batchId": "36db590d-2945-4bd3-a16a-640289316fcb",
  "streamId": "05520822-f473-4ad2-bf0c-a04d15df4880",
  "sequence": 1,
  "eventCount": 2,
  "receivedAt": "2026-10-28T00:00:02Z"
}
```

Responses use `Cache-Control: no-store`. Retrying a stored batch does not change
`receivedAt`, insert events again or extend the session's activity timestamp.

### Wire contract and limits

`schemaVersion` must be integer 1. `batchId` and `streamId` must be canonical
UUID strings. `sequence` starts at 1 per stream and must not exceed JavaScript's
safe integer maximum (9007199254740991). Out-of-order batches and sequence gaps
are accepted; sequence does not prove complete delivery. Use a new stream UUID
when creating a new page collection instance, including after a refresh. Batch
UUIDs, stream UUIDs, sequence and event contents must remain unchanged on retry.

Every event requires `type`, `timestamp`, `sessionElapsedMs` and `pageId`.
`timestamp` is integer epoch milliseconds in the range 0–253402300799999
(1970 through year 9999). `sessionElapsedMs` is finite and may be negative or
equal to another event's value. The backend does not compare client timestamps
against its own clock or invent relative times. Use the same `SessionClock`
instance as gaze collection for both time fields.

`pageId` is a nonblank string of at most 128 UTF-16 code units. Optional
`componentId` and `componentType` may be omitted or null; when present they must
be nonblank and at most 255/128 UTF-16 code units respectively. Null bytes and
unpaired Unicode surrogates are rejected. These attribution values are supplied
by the browser; the server does not infer or verify them against a rendered DOM.

| Event type | Required event-specific fields | Meaning |
|---|---|---|
| `click` | `x`, `y` | Finite viewport CSS-pixel coordinates; negative values are retained. |
| `keydown` | `code` | `KeyboardEvent.code`, 1–64 ASCII letters/digits, starting with a letter. |
| `mousemove` | `x`, `y` | Same coordinate contract as click. |
| `scroll` | `scrollX`, `scrollY` | Finite scroll offsets; negative offsets are allowed. |
| `visibilitychange` | `hidden` | JSON boolean corresponding to document visibility. |

Fields belonging to another event type may be omitted or null but must not
carry values. Unknown fields are rejected, including `key`, `value`, password
fields, arbitrary DOM content and nested custom payloads. Numeric/boolean strings
are not coerced, and trailing JSON values are rejected. The backend accepts all
five event types; the current frontend collector only produces click/keydown.
This does not imply that the other frontend listeners have been implemented.

Defaults are 1000 events and 1048576 bytes (1 MiB) per request. The byte limit is
checked on a bounded input stream before JSON parsing; the service also bounds
the canonical batch representation. Optional environment overrides:

```dotenv
# Maximum event count per transaction; supported configuration range is 1–10000.
BEHAVIOR_MAX_EVENTS_PER_BATCH=1000
# Maximum raw/canonical UTF-8 batch bytes; supported range is 1024–8388608.
BEHAVIOR_MAX_PAYLOAD_BYTES=1048576
```

### Persistence, permissions and retry handling

V23 creates `behavior_batches` and `behavior_events`. A batch is unique by
`(session_id, batch_id)` and by `(session_id, stream_id, sequence)`. Events retain
their original array order through `event_index`; common columns contain the
type, timestamps, page and component identifiers, while JSONB stores only the
validated event-specific payload. No derived behavioral metrics are computed.

The semantic SHA-256 digest ignores JSON spacing/property order, normalizes UUID
case and treats omitted/null optional attribution fields equivalently. Reusing
a batch ID for different contents, or a stream sequence for a different batch,
returns `409 PARTICIPANT_IDEMPOTENCY_CONFLICT` rather than duplicating data.

Uploads call the existing M5 collection policy and activity port in the same
transaction as both inserts, preserving Study-before-session lock order.
The session must have consented, remain `IN_PROGRESS`, and be in `BROWSING` or
`QUESTIONNAIRE`; its study must be `COLLECTING`. The questionnaire phase permits
late browsing-batch delivery. Eye tracking may be disabled. All events and the
activity update commit together or roll back together. The check runs before
replay lookup: even previously stored batches cannot be retried after the study
closes or the session ends. A close/delete waits for an upload already holding
the collection locks. Permanent study deletion cascades through sessions,
batches and events without a separate cleanup operation.

| HTTP status | Meaning and frontend handling |
|---|---|
| 200 / 201 | Batch acknowledged; remove only that confirmed batch from the local queue. |
| 400 | `BEHAVIOR_BATCH_INVALID`; correct the request, do not retry unchanged. |
| 401 | Participant token missing/invalid or session deleted; stop using this token. |
| 404 | `PARTICIPANT_SESSION_NOT_FOUND`. |
| 409 | Identity conflict or invalid/ended session; inspect the returned code. |
| 410 | `STUDY_CLOSED`; stop collecting and retrying. |
| 413 | `BEHAVIOR_BATCH_TOO_LARGE`; split the unaccepted data into smaller batches. |
| 415 | Send `Content-Type: application/json`. |

### Frontend handoff

Keep collection separate from upload. Add shared-clock/page context when events
are captured, isolate queues by session, and cut immutable batches with stable
IDs before sending. A practical initial client policy is every 2 seconds or 200
events; this cadence is not enforced by the backend. Do not clear the entire
collector array when a request completes: new events may have arrived meanwhile.
Remove only the acknowledged batch and retain failed batches for retry while
the session remains writable. Normal completion must wait for required batch
acknowledgements before ending the session.

The current in-memory array does not survive refreshes. IndexedDB queueing,
network retry behavior, mousemove throttling at 10–20 Hz, the remaining event
listeners and browser-to-database integration remain frontend work. New stream
IDs avoid identity collisions after reload; they do not recover lost memory.
No final-delivery manifest is implemented by the upload module. Exported behavior
reflects saved records, not an assertion that every browser event was delivered.

Focused backend verification:

```powershell
.\mvnw.cmd "-Dtest=BehaviorBatchValidationTest,BehaviorBatchServiceTest,BehaviorControllerTest,BehaviorTransactionTest,BehaviorPostgresIntegrationTest" test
```

Local H2 tests verify rollback/cascade behavior with a reduced schema and do not
prove PostgreSQL JSONB semantics. The container suite applies actual migrations
and exercises JSONB, concurrent deduplication, lifecycle rejection and close/delete
serialization. It is explicitly skipped when Docker is unavailable.
