# Spring Boot Angular Auth Starter

Reusable open-source starter for a Spring Boot authentication backend and Angular authentication frontend. It uses session-based authentication, CSRF protection, and RBAC as the default full-stack integration model.

## Why this project exists

This project was extracted from a production-proven authentication and user-management foundation and generalized into a reusable open-source starter.

The goal is to help teams bootstrap enterprise-grade authentication, authorization, invitation, and password-management workflows using Spring Boot and Angular.

It focuses on practical backend/frontend integration patterns rather than being a minimal demo.

## Purpose

This project provides a reusable full-stack baseline for authentication, authorization, user lifecycle management, invitations, password setup and reset, and account notification history.

## Features

- Spring Boot backend with Spring Security, Spring GraphQL, JDBC, and Flyway
- Angular frontend with login, logout, route guards, account page, user management, and notification history
- Session-based authentication with server-side sessions
- CSRF protection for authenticated backend requests
- RBAC role model: `SUPERADMIN`, `ORG_ADMIN`, `USER`
- Current user, organization context, and membership queries
- User management for admin roles
- Invitation flow with hashed single-use tokens
- First-login password setup through invitation links
- Forgot password request flow
- Password reset flow with hashed tokens
- Notification framework with local mock or generic SMTP email providers and notification history
- Docker local development setup with PostgreSQL and the backend through Docker Compose

## Tech Stack

- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring GraphQL, JDBC, Flyway
- Database: PostgreSQL
- Frontend: Angular 20, Apollo Angular, RxJS, lucide-angular
- Tooling: Gradle wrapper, pnpm 10.6.5, Node 22.14.0

## Local Setup

Start PostgreSQL and the backend with Docker Compose:

```sh
cp .env.example .env
docker compose up --build
```

Run the frontend separately:

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

Open `http://localhost:4200`. The backend runs at `http://localhost:8080`.

Backend without Docker:

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Environment Variables

See `.env.example` for local defaults. Important variables:

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_BASELINE_AUTH_USERNAME`
- `AUTH_STARTER_BASELINE_AUTH_PASSWORD`
- `AUTH_STARTER_BASELINE_AUTH_ROLE`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`
- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`

Use `local-mock` for development without a mail server, or `smtp` with the dummy SMTP variables in `.env.example` for a local mail catcher.

## Default Demo Users

Baseline local login:

- `operator@authstarter.local`
- `authstarter-local-password`
- `SUPERADMIN`

Additional local profile users configured in `application-local.yml`:

- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

## API Overview

Public GraphQL operations:

- `readiness`
- `currentSession`
- `login`
- `logout`
- `acceptUserInvite`
- `requestPasswordReset`
- `resetPassword`

Authenticated operations:

- `currentUserProfile`
- `currentOrganizationContext`
- `foundationOrganizations`
- `rbacBaseline`
- `notificationEvents`
- `changeOwnPassword`

Admin operations requiring `SUPERADMIN` or `ORG_ADMIN`:

- `adminManagementBaseline`
- `adminCreateUser`
- `adminUpdateUser`

CSRF bootstrap endpoint:

- `GET /auth/csrf`

Health endpoints:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

## Verification

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

## Roadmap

- Add richer email templates and preview tooling
- Add password policy configuration
- Add refresh/remember-me options if needed
- Add richer organization management UI
- Add optional JWT resource-server mode
- Add end-to-end tests for invitation and password reset
- Add CI publishing templates for OSS releases

## OSS Disclaimer

This starter is provided as a reusable starting point, not as a complete deployed identity platform. Review security, compliance, email delivery, password policy, deployment, monitoring, and data-retention requirements before using it in a real system.

## License

MIT. See [LICENSE](./LICENSE).
