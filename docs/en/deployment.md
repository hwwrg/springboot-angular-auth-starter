[Documentation index](./README.md)

# Deployment

This repository contains a local development Compose setup and a backend Dockerfile. It does not currently include production infrastructure or a separate frontend container image.

## Backend Image

[../../backend/Dockerfile](../../backend/Dockerfile) builds the Spring Boot jar with Java 21, then runs it on a Java 21 JRE image. The container exposes port `8080`.

The runtime image runs as a non-root user and does not install curl or other extra runtime tools.

The entrypoint runs:

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar
```

The image starts with the safe base `application.yml` defaults unless `SPRING_PROFILES_ACTIVE` is explicitly provided. It does not enable the local profile or local demo credentials by default.

## Backend Runtime Configuration

Configure at least:

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE`
- `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`

For SMTP delivery, also configure:

- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`
- `AUTH_STARTER_SMTP_USERNAME`
- `AUTH_STARTER_SMTP_PASSWORD`
- `AUTH_STARTER_SMTP_AUTH`
- `AUTH_STARTER_SMTP_START_TLS`

Review [../../.env.example](../../.env.example) and [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml) before deploying. [../../.env.local.example](../../.env.local.example) is local-only and must not be used in deployed environments.

Break-glass authentication defaults to disabled. Do not deploy the local demo credentials from `application-local.yml`, `application-dev.yml`, `.env.local.example`, or Docker Compose.

Public auth mutations include a basic in-memory rate limiter. Production or multi-instance deployments should replace or back it with distributed storage such as Redis.

Keep GraphQL introspection disabled in deployed profiles unless you have an explicit operational need. Configure depth, complexity, and request body size limits with the `AUTH_STARTER_GRAPHQL_*` variables.

## Frontend Build

Build the Angular app with:

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 build
```

The development environment currently points to `http://localhost:8080`. Runtime config support exists in `RuntimeConfigService`, and [../../frontend/public/config.template.json](../../frontend/public/config.template.json) shows the expected shape, but an environment file must set `runtimeConfigPath` before the app loads remote runtime config.

## Local Compose Scope

[../../docker-compose.yml](../../docker-compose.yml) starts PostgreSQL and the backend for local development. It is not a production deployment template.

The Compose PostgreSQL and backend ports are bound to `127.0.0.1` for local use.

Compose explicitly sets `SPRING_PROFILES_ACTIVE=local` for local demo credentials. Do not copy that setting into deployed environments unless you intentionally maintain a separate deployed profile.

Before using this starter in a deployed system, review cookie security, HTTPS, CORS origins, SMTP, password policy, monitoring, backup, retention, and operational access.
