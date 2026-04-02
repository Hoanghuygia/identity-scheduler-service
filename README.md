# Auth Service Template

Production-minded but lightweight Spring Boot 3 auth microservice starter.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA + Hibernate
- PostgreSQL + Flyway
- JWT architecture placeholders (RS256-ready structure)
- BCrypt password hashing
- Validation, Mail, Actuator, Micrometer
- OpenAPI (springdoc)
- Lombok + MapStruct
- Docker + GitHub Actions CI

## Current Scope (Template)

This project is a scaffold only. Endpoint responses are stubs and include TODO markers for business logic.

Implemented starter endpoints under `/api/v1/auth`:

- `POST /register`
- `POST /login`
- `POST /refresh`
- `POST /forgot-password`
- `POST /reset-password`
- `GET /verify-email`
- `GET /me`
- `GET /health-test`
- `GET /admin-test`

## Quick Start

```bash
cp .env.example .env
mvn spring-boot:run
```

OpenAPI UI:

- `http://localhost:8080/swagger-ui.html`

Actuator health:

- `http://localhost:8080/actuator/health`

## Build and Test

```bash
mvn clean verify
```

## Docker

```bash
docker build -t authservice:local .
docker run --rm -p 8080:8080 --env-file .env authservice:local
```

## Notes

- Security and JWT flows are intentionally partial with TODO hooks.
- Flyway migrations include schema and role seed data.
- Request and trace IDs are propagated via headers and MDC logging.

