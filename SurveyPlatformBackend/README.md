# SurveyPlatform Backend

Backend service for SurveyPlatform, a University of Sydney COMP5703 project. Built with Java, Spring Boot, and PostgreSQL, it provides the foundation for developing the survey platform.

The backend currently includes researcher authentication and authorization, Study and feed-template APIs, the reusable question bank, and versioned questionnaire-draft APIs with FR38 single-choice and scale branching.

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
- V6: reusable question bank and stable question-option identities
- V7: versioned questionnaire drafts and stable ordered item identities
- V8: deterministic questionnaire branch rules

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
4. Install a Docker-compatible container runtime to run PostgreSQL integration tests. Without Docker, container-backed test classes are skipped; CI and final verification should run them with Docker available.

All database values in this document are examples. Replace them with the values for your environment. Do not store actual passwords in this README, `application.yaml`, or shared run configurations.

| Environment Variable | Example Value | Description |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/example_database` | JDBC URL containing the host, port, and database name |
| `SPRING_DATASOURCE_USERNAME` | `example_user` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | `replace-with-local-password` | Database password |
| `JWT_SECRET` | `replace-with-base64-encoded-secret` | Base64-encoded secret used to sign and verify JWT access tokens |
| `TURNSTILE_SECRET` | `your-turnstile-secret` | Cloudflare Turnstile server-side verification secret |

`application.yaml` reads these values from environment variables. The datasource variables configure the PostgreSQL connection, while `JWT_SECRET` and `TURNSTILE_SECRET` are security secrets used by the application. These values must be kept separate from platform user credentials and must not be committed to the repository.

Copy `.env.example` to a local `.env` file to store your configuration. Spring Boot and Maven do not load `.env` files automatically.

## Running with IntelliJ IDEA

1. Import the backend directory or its `pom.xml` as a Maven project. Select JDK 17 and the project Maven Wrapper.
2. Copy `.env.example` to `.env` in the backend directory and configure the database, JWT secret, and Turnstile secret values.
3. Open **Run → Edit Configurations** and select or create the run configuration for `SurveyPlatformBackendApplication`.
4. In **Environment variables**, use **Browse for .env files and scripts** to select the backend `.env` file. If the field is hidden, enable it through **Modify options → Environment variables**.
5. Apply the configuration and run the application.

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

.\mvnw.cmd spring-boot:run
```
> The actual Cloudflare Turnstile secret must not be committed to the repository.
> Team members should obtain the development secret through a private channel and store it only in their local `.env` file.

These variables apply only to the current PowerShell session and its child processes. They remain available when restarting the application in the same session, but must be set again in a new session. Press `Ctrl + C` to stop the application.

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

With Docker available, the full command starts an isolated PostgreSQL container, applies Flyway V1 through V8, and runs repository and concurrency integration tests. Machine-dependent timing checks are opt-in:

```powershell
.\mvnw.cmd -Pperformance test
```

## Question bank and questionnaire drafts

M4 is contained under the top-level `survey` package. Question-bank APIs are under
`/api/questions`; questionnaire composition remains a `survey.questionnaire`
subdomain and exposes only:

```text
GET /api/studies/{studyId}/questionnaire
PUT /api/studies/{studyId}/questionnaire
```

`PUT` submits the complete desired item order. Existing `itemId` values must be
retained when reordering or replacing items. A first save uses a null
`expectedVersion`; subsequent changes submit the version returned by `GET`.
Identical retries are idempotent and do not advance the questionnaire version.

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

Question updates retain explicitly submitted option IDs. Removing an option used
by a branch rule, changing a rule-bearing question to an incompatible type, or
excluding a scale trigger returns HTTP 409. Answer-domain changes revalidate every
affected draft questionnaire. Deleting a question clears its outgoing rules,
retains incoming references to the now-missing item, and makes that draft visibly
invalid until repaired. `GET` returns stable item and target IDs, per-item
reference status, `valid`, and structured `validationIssues`.

This module currently covers authoring-time draft consistency. Participant
runtime execution, publishing snapshots, and a visual questionnaire editor are
separate follow-up work; no publish transition should be enabled without the
snapshot boundary.

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
contract. Do not rewrite V5 after it has been applied. Feed retrieval/editing,
legacy-study initialization and publishing are separate follow-up work.
