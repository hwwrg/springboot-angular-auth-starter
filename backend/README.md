# Backend

Spring Boot GraphQL backend for `springboot-angular-auth-starter`.

## Run

```sh
./gradlew bootRun --args='--spring.profiles.active=local'
```

The backend expects PostgreSQL by default at `jdbc:postgresql://localhost:5432/authstarter`.

The `local` profile enables local-only demo break-glass credentials. Break-glass authentication is disabled by default in `application.yml`.

The backend Docker image does not set `SPRING_PROFILES_ACTIVE=local` by default. Docker Compose sets that profile only for local demo use.

## API Documentation

With the backend running locally, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- GraphQL endpoint: `http://localhost:8080/graphql`

OpenAPI documents REST support endpoints such as `GET /auth/csrf`. Auth, RBAC, invitation, password-reset, notification, and user-management workflows are exposed through GraphQL and documented in [`schema.graphqls`](./src/main/resources/graphql/schema.graphqls) and the root API guide.

## Test

```sh
./gradlew test
./gradlew bootJar
```

## Scope

Kept: authentication, CSRF, server-side sessions, RBAC, current user context, user management, invitations, password reset, Flyway migrations, and local mock/generic SMTP account email notifications.

Out of scope: application-specific workflows, deployment infrastructure, and generated runtime artifacts.
