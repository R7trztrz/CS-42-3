# SurveyPlatform Backend

Backend service for SurveyPlatform, a University of Sydney COMP5703 project. Built with Java, Spring Boot, and PostgreSQL, it provides the foundation for developing the survey platform.

The project is currently at the initialization stage. The application entry point, database connection, dependencies, and application context test are in place. Business APIs, user login, JWT issuance, and JWT validation configuration have not yet been implemented.

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
| Spring Security OAuth2 Resource Server | Managed by Spring Boot | Spring Security integration and Bearer token / JWT validation support; security configuration is still required |
| Validation | Managed by Spring Boot | Input validation using Jakarta Bean Validation |
| springdoc OpenAPI | 3.1.0 | OpenAPI documentation generation and Swagger UI |
| Apache Commons CSV | 1.14.1 | CSV parsing, writing, and field escaping |
| Actuator | Managed by Spring Boot | Health checks and application management endpoints |
| Lombok | Managed by Spring Boot | Compile-time generation of constructors, accessors, and other boilerplate |
| DevTools | Managed by Spring Boot | Automatic application restarts after recompilation during development |
| Spring Boot Test Starters | Managed by Spring Boot | Test support for MVC, JPA, security, validation, Flyway, and Actuator |

Refer to `pom.xml` for the dependency definitions.

## Project Structure and Architecture

```text
SurveyPlatformBackend/
├── .mvn/wrapper/                  # Maven Wrapper configuration
├── src/main/java/com/cs_42_3/surveyplatformbackend/
│   └── SurveyPlatformBackendApplication.java
├── src/main/resources/
│   └── application.yaml          # Application configuration
├── src/test/java/com/cs_42_3/surveyplatformbackend/
│   └── SurveyPlatformBackendApplicationTests.java
├── .env.example                  # Sanitized environment template
├── .gitignore
├── mvnw                          # Linux/macOS Maven Wrapper
├── mvnw.cmd                      # Windows Maven Wrapper
└── pom.xml
```

The current setting, `spring.jpa.hibernate.ddl-auto=validate`, instructs Hibernate to validate entity mappings against the database schema without creating or updating tables. Add future migration scripts under `src/main/resources/db/migration/`, using names such as `V1__create_initial_tables.sql`. This directory and the application migration scripts have not yet been added.

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

`application.yaml` reads these variables through placeholders such as `${SPRING_DATASOURCE_URL}`. These credentials authenticate database connections; they are separate from platform user credentials.

Copy `.env.example` to a local `.env` file to store your configuration. Spring Boot and Maven do not load `.env` files automatically.

## Running with IntelliJ IDEA

1. Import the backend directory or its `pom.xml` as a Maven project. Select JDK 17 and the project Maven Wrapper.
2. Copy `.env.example` to `.env` in the backend directory and enter your database connection values.
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

.\mvnw.cmd spring-boot:run
```

These variables apply only to the current PowerShell session and its child processes. They remain available when restarting the application in the same session, but must be set again in a new session. Press `Ctrl + C` to stop the application.

Entering a password directly in a command may save it in the terminal history. To enter the password through a credential prompt instead, replace the password assignment above with:

```powershell
$credential = Get-Credential -UserName $env:SPRING_DATASOURCE_USERNAME -Message 'Enter the database password'
$env:SPRING_DATASOURCE_PASSWORD = $credential.GetNetworkCredential().Password
Remove-Variable credential
```
