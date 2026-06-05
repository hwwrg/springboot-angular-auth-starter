[Documentation index](./README.md)

# Architecture

## Backend

The backend is a Spring Boot application under [../../backend](../../backend). Important areas:

- `com.example.authstarter.auth`: login, logout, session payloads, password change, invitation acceptance, and password reset
- `com.example.authstarter.config.security`: Spring Security, CORS, CSRF, and GraphQL authorization
- `com.example.authstarter.foundation`: current user, organization, workspace, and membership context
- `com.example.authstarter.admin`: admin user management
- `com.example.authstarter.notification`: notification events and email provider selection
- `backend/src/main/resources/db/migration`: Flyway migrations
- `backend/src/main/resources/graphql/schema.graphqls`: GraphQL schema

The backend uses JDBC and Flyway against PostgreSQL. Local migrations create workspaces, organizations, users, memberships, user credentials, security tokens, and notification events.

## Frontend

The frontend is an Angular application under [../../frontend](../../frontend). Important areas:

- `src/app/core/auth`: session bootstrap, login/logout, CSRF fetch, password flows
- `src/app/core/guards`: app-shell, guest-shell, and role access guards
- `src/app/core/graphql`: Apollo Angular client configuration
- `src/app/core/runtime-config`: optional runtime configuration loading
- `src/app/features/auth`: login, invite acceptance, forgot password, and reset password pages
- `src/app/features/admin`: admin user management page
- `src/app/features/notifications`: notification history page

The frontend sends GraphQL requests with `withCredentials: true` so browser requests include the backend session cookie.

## Request Flow

1. The frontend calls `GET /auth/csrf`.
2. The backend returns a CSRF payload and also uses the `XSRF-TOKEN` cookie.
3. Unsafe frontend requests include the `X-XSRF-TOKEN` header.
4. Login calls `login` on `POST /graphql`.
5. The backend stores the Spring Security context in the server-side session.
6. Authenticated GraphQL operations use the `AUTH_STARTER_SESSION` cookie.

## Roles

The current role enum is:

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Admin routes and GraphQL operations require `SUPERADMIN` or `ORG_ADMIN`. Only `SUPERADMIN` can assign or modify `SUPERADMIN`; `ORG_ADMIN` cannot modify `SUPERADMIN` users, cannot change its own role/status, and cannot assign `ORG_ADMIN` unless the backend policy explicitly allows it.
