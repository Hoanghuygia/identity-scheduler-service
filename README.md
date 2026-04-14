# Auth Service

Production-ready Spring Boot 3 authentication microservice with JWT, role-based access control, and comprehensive audit logging.

## Tech Stack

- **Java 21** + **Spring Boot 3.3.4**
- **Spring Security** with stateless JWT authentication (RS256-ready)
- **Spring Data JPA** + **Hibernate**
- **PostgreSQL** + **Flyway** migrations
- **BCrypt** password hashing
- **Nimbus JOSE+JWT** for JWT operations
- **Jakarta Validation** for request validation
- **Spring Mail** with HTML email templates
- **User-Agent parser** for client/device detection
- **Micrometer** + **Prometheus** metrics
- **SpringDoc OpenAPI** for API documentation
- **Lombok** + **MapStruct**
- **Docker** support

## Features

### Authentication
- User registration with email verification
- Login with email/password
- JWT access tokens (15 min expiry) + refresh tokens (30 day expiry)
- Password reset via email
- Email verification flow
- Logout with token invalidation
- Role-based access control (USER, ADMIN, SUPER_ADMIN)

### Security
- Stateless JWT authentication
- BCrypt password hashing (strength: 12)
- Request tracing via custom headers
- Structured MDC logging
- Global exception handling
- Secure password validation rules

### Audit & Monitoring
- Complete audit logging for all auth events
- Client info tracking (IP, device, browser, OS)
- Request lifecycle logging
- Actuator health & Prometheus metrics

## API Endpoints

### Public Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | User login |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| POST | `/api/v1/auth/forgot-password` | Request password reset |
| POST | `/api/v1/auth/reset-password` | Reset password with token |
| GET | `/api/v1/auth/verify-email` | Verify email address |

### Protected Endpoints (require authentication)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/me` | Get current user info |
| POST | `/api/v1/auth/logout` | Logout and invalidate tokens |

### Admin Endpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/auth/admin/users` | List all users |
| GET | `/api/v1/auth/admin/users/{id}` | Get user by ID |
| PATCH | `/api/v1/auth/admin/users/{id}/status` | Update user status |

### Health & Info
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/actuator/prometheus` | Prometheus metrics |
| GET | `/swagger-ui.html` | OpenAPI documentation |

## Quick Start

### Prerequisites
- Java 21
- PostgreSQL 15+
- Maven 3.9+

### Configuration

```bash
cp .env.example .env
# Edit .env with your database and mail settings
```

### Run

```bash
mvn spring-boot:run
```

### Docker

```bash
docker build -t authservice:local .
docker run --rm -p 8080:8080 --env-file .env authservice:local
```

## Build & Test

```bash
# Full build with tests
mvn clean verify

# Run tests only
mvn test

# Compile only
mvn compile

# Package JAR
mvn package -DskipTests
```

## Project Structure

```
src/main/java/com/example/authservice/
├── auth/              # Authentication (controller, service, DTOs, mapper, validator)
├── audit/            # Audit logging (entity, repository, service)
├── common/           # Cross-cutting: exceptions, response wrappers, filters, logging, util
├── config/           # AppProperties, MailConfig, OpenApiConfig
├── mail/             # Email service (template builder)
├── role/             # Role entity, repository, service
├── security/         # Spring Security config, JWT filter/service
├── token/            # Refresh, password-reset, email-verification tokens
└── user/             # User entity, DTOs, repository, service
```

## Database Schema

Migrations are managed by Flyway in `src/main/resources/db/migration/`:

- `V1__init_auth_schema.sql` - Core tables (users, roles, user_roles)
- `V2__init_token_schema.sql` - Token tables (refresh_tokens, password_reset_tokens, email_verification_tokens)
- `V3__init_audit_schema.sql` - Audit logging table

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/authservice` |
| `DATABASE_USERNAME` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | - |
| `JWT_SECRET_KEY` | JWT signing key (Base64) | - |
| `JWT_ACCESS_TOKEN_EXPIRY` | Access token expiry (ms) | `900000` (15 min) |
| `JWT_REFRESH_TOKEN_EXPIRY` | Refresh token expiry (ms) | `2592000000` (30 days) |
| `MAIL_HOST` | SMTP host | `smtp.example.com` |
| `MAIL_USERNAME` | SMTP username | - |
| `MAIL_PASSWORD` | SMTP password | - |
| `APP_BASE_URL` | Base URL for email links | `http://localhost:8080` |
| `APP_FRONTEND_URL` | Frontend URL for redirects | `http://localhost:3000` |
