# Backend

Spring Boot GraphQL backend for `springboot-angular-auth-starter`.

## Run

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

The backend expects PostgreSQL by default at `jdbc:postgresql://localhost:5432/authstarter`.

The `local` profile enables local-only demo break-glass credentials. Break-glass authentication is disabled by default in `application.yml`.

## Test

```sh
./gradlew test
./gradlew bootJar
```

## Scope

Kept: authentication, CSRF, server-side sessions, RBAC, current user context, user management, invitations, password reset, Flyway migrations, and local mock/generic SMTP account email notifications.

Out of scope: application-specific workflows, deployment infrastructure, and generated runtime artifacts.
