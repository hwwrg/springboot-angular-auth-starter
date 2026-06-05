[Documentation index](./README.md)

# Deployment

This repository contains a local development Compose setup and a backend Dockerfile. It does not currently include production infrastructure or a separate frontend container image.

## Backend Image

[../../backend/Dockerfile](../../backend/Dockerfile) builds the Spring Boot jar with Java 21, then runs it on a Java 21 JRE image. The container exposes port `8080`.

The entrypoint runs:

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}
```

Set `SPRING_PROFILES_ACTIVE` explicitly outside local development.

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

Review [../../.env.example](../../.env.example) and [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml) before deploying.

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

Before using this starter in a deployed system, review cookie security, HTTPS, CORS origins, SMTP, password policy, monitoring, backup, retention, and operational access.
