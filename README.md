# CodeRank

A Spring Boot service that lets authenticated users submit source code, executes it asynchronously inside short-lived Docker containers, and exposes the result via a REST API. Submission jobs are dispatched through Apache Kafka so that web request handling is decoupled from sandboxed execution.

## Tech stack

- **Java 17**, **Spring Boot 4.x** (Web MVC, Data JPA, Security, Validation)
- **MySQL 8** for persistence
- **Apache Kafka** for the submission queue
- **Docker** as the code-execution sandbox
- **JJWT 0.12.x** for stateless JWT authentication
- **Bucket4j** for per-user rate limiting
- **Gradle** build (wrapper checked in)

## Architecture at a glance

```
client ──► UserController        (POST /auth/v1/register, /auth/v1/login)
client ──► CodeSubmissionController
            │  POST /submit/v1/submit  ──► CodeSubmissionService
            │                                │
            │                                ├─► save(submission, status=PENDING)
            │                                └─► KafkaProducer  ──► topic: coderank-submission-topic
            │
            └─ GET  /submit/v1/result/{id} ◄── CodeSubmissionService.getSubmissionResult

KafkaConsumer ──► DockerExecutorService.executeCode (@Async, dockerExecutorPool)
                  │
                  ├─► docker run --rm --network none --memory 128m --cpus 0.5 …
                  ├─► capture stdout/stderr (truncated to 65k chars)
                  └─► save(submission, status=SUCCESS|FAILED)
```

`JwtAuthenticationFilter` runs inside Spring Security's filter chain and authenticates every non-`/auth/v1/login`/`/auth/v1/register` request from the `Authorization: Bearer <jwt>` header.

## Prerequisites

You need all four of these running locally before starting the app:

1. **Java 17** (`java -version` should report 17.x)
2. **MySQL** on `localhost:3306` with a database named `coderank`
3. **Kafka** broker reachable at `localhost:9092` (auto topic creation enabled, or rely on the `NewTopic` bean to create `coderank-submission-topic` on startup)
4. **Docker daemon** running, with the language images pulled:

   ```bash
   docker pull python:latest
   docker pull openjdk:latest
   docker pull gcc:latest
   ```

## Configuration

All runtime configuration lives in `src/main/resources/application.properties`. The defaults assume a local MySQL with user `root` / password `Ashu@123`. Adjust as needed before running.

| Property | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/coderank` | DB must already exist |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/extends tables but will not widen existing column types |
| `JWT_SECRET_KEY` | (long dev string) | Must be ≥32 bytes when UTF-8 encoded (HS256 minimum). Override via env var in production |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Producer + consumer use String (de)serializer |

> **Note on DDL.** `ddl-auto=update` only **adds** columns/tables — it does not change existing column types. If you previously ran an older revision of the app, run the migration in [Database migrations](#database-migrations) once before starting the new build.

## Running

```bash
./gradlew bootRun
```

Look for `Started CoderankApplication in …`. The HTTP server binds on port 8080.

## API

Base URL: `http://localhost:8080`

### 1. Register

```bash
curl -i -X POST http://localhost:8080/auth/v1/register \
  -H 'Content-Type: application/json' \
  -d '{
    "userName": "alice",
    "password": "alice123",
    "email": "alice@example.com"
  }'
```

`201 CREATED` with the saved user record.

Validation: `userName` 3–20 chars, `password` ≥6 chars, `email` valid. `userName` and `email` are unique.

### 2. Login

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/v1/login \
  -H 'Content-Type: application/json' \
  -d '{"userName":"alice","password":"alice123"}')

echo "$TOKEN"
```

`200 OK` with the raw JWT in the response body. The token has a 24-hour lifetime, claims `sub`, `roles=ROLE_USER`, and `userName`.

### 3. Submit code

```bash
SUBMISSION_ID=$(curl -s -X POST http://localhost:8080/submit/v1/submit \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "language": "python",
    "code": "print(\"hello from coderank\")",
    "stdin": ""
  }')

echo "$SUBMISSION_ID"
```

`201 CREATED` with the submission UUID. Allowed `language` values: `python | java | javascript | node | cpp | csharp` (only `python`, `java`, and `cpp` are wired to Docker images today).

Per-user rate limit: **10 submissions / minute**.

### 4. Fetch the result

```bash
curl -i -X GET "http://localhost:8080/submit/v1/result/$SUBMISSION_ID" \
  -H "Authorization: Bearer $TOKEN"
```

`200 OK` with `{ "stdout": …, "stderr": … }`. Both fields are `null` until the worker has finished. Re-poll until the submission's status flips to `SUCCESS` or `FAILED`.

Execution timeout: **30 seconds**. Outputs longer than 65,000 characters are truncated with a `…[truncated]` suffix.

## Database migrations

If you have an existing `coderank` database from before the latest changes, run this once to widen `code`, `error`, and `output` columns from `VARCHAR(255)` to `LONGTEXT`:

```sql
ALTER TABLE code_submission
  MODIFY code   LONGTEXT NULL,
  MODIFY error  LONGTEXT NULL,
  MODIFY output LONGTEXT NULL;
```

Fresh databases get the correct column types automatically from the entity annotations.

## Project layout

```
src/main/java/com/ashutosh/coderank
├── CoderankApplication.java
├── Dto/                       # Request/response DTOs with Bean Validation
├── config/                    # Spring Security, JWT filter, Kafka topic, rate limiting
├── constant/                  # Static constants (error codes, roles, JWT keys)
├── controller/                # REST controllers + global @ExceptionHandler
├── exceptions/                # Domain exception classes
├── model/                     # JPA entities (Users, CodeSubmission)
├── repository/                # Spring Data JPA repositories
├── schedulerconfig/           # @Async executor configuration
├── service/                   # UserService, CodeSubmissionService, Kafka, Docker
└── util/                      # TokenUtil (JWT issue/verify)
```

## Build & test

```bash
./gradlew clean build
./gradlew test
```

## Troubleshooting

- **`Data too long for column 'error'`** — your DB still has `VARCHAR(255)` columns. Run the SQL in [Database migrations](#database-migrations).
- **Submit returns `403`** — restart the app. The `JwtAuthenticationFilter` is registered both as a `@Component` and inside the security chain; a `FilterRegistrationBean` with `setEnabled(false)` in `AuthConfig` disables the duplicate, but it only takes effect after a restart.
- **Submissions hang or always time out** — confirm the Docker daemon is running and the language image is pulled (`docker images`). The first run on a missing image will exceed the 30 s execution timeout while pulling.
- **Login returns `500`** — usually a stale process running an older binary. Stop it and re-run `./gradlew bootRun`.
