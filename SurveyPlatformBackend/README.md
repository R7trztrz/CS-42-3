# SurveyPlatform Backend

Backend service for SurveyPlatform, a University of Sydney COMP5703 project. Built with Java, Spring Boot, and PostgreSQL, it provides the foundation for developing the survey platform.

The backend currently provides the core researcher authentication and authorization flow together with the Study API foundation.

Implemented functionality includes researcher registration and login, BCrypt password hashing, JWT issuance and validation, stateless request authentication, RESEARCHER role-based authorization, authenticated researcher identity handling, password change, Cloudflare Turnstile verification for registration, and rate limiting for authentication endpoints.

The backend also exposes OpenAPI documentation through Swagger UI and uses Flyway migrations to manage the PostgreSQL schema.

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
│   └── SurveyPlatformBackendApplication.java
├── src/main/resources/
│   ├── application.yaml           # Application configuration
│   └── db/migration/              # Flyway database migrations
├── src/test/
├── .env.example                   # Sanitized environment template
├── .gitignore
├── mvnw
├── mvnw.cmd
└── pom.xml
```

The project uses Flyway for database schema management. Migration scripts are stored under `src/main/resources/db/migration/`.

The current setting, `spring.jpa.hibernate.ddl-auto=validate`, instructs Hibernate to validate entity mappings against the Flyway-managed database schema without automatically creating or modifying database tables.

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

Questionnaire readiness checks and question snapshots are temporarily bypassed,
even when questionnaireEnabled is true. An English TODO marks this integration
work. Detailed Craft.js validation is also deferred: publication does not yet
guarantee renderability or satisfy questionnaire readiness/snapshot criteria.

Configure the frontend base URL (defaults to the local frontend origin):

```powershell
$env:APP_PARTICIPANT_BASE_URL = "http://localhost:5173"
```

The backend appends `/participate/{token}`. Set the deployed frontend base URL
in production. The frontend must implement this page separately.

`GET /api/participation/{token}` requires no researcher JWT. It returns title,
description, runtime switches, feed theme and JSON content, without management
metadata. Only this GET route is anonymously permitted. Responses use
`Cache-Control: no-store`; every read checks the current study state.
Invalid links return 404 PARTICIPATION_NOT_FOUND; CLOSED returns 410 STUDY_CLOSED.

Publishing returns 404 STUDY_NOT_FOUND for missing or foreign-owned studies, or
409 STUDY_NOT_PUBLISHABLE, STUDY_VERSION_CONFLICT or FEED_NOT_READY.
Invalid versions return 400; framework error normalization is unchanged.

Participant sessions, data submission, questionnaire execution and the close
command remain separate features. Future questionnaire saves must use the same
study lock and DRAFT-only rule; future participation writes must recheck state.

Run the focused suite (isolated H2 storage and production security filter rules):

```powershell
.\mvnw.cmd -B -ntp "-Dtest=StudyPublicationApiTest,StudyApiTest,StudyPersistenceTest,FeedApiTest" test
```

Live PostgreSQL/Flyway and frontend participation-page integration require
separate verification.
