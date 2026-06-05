[Index documentation](./README.md)

# Architecture

## Backend

Le backend est une Spring Boot application dans [../../backend](../../backend). Zones importantes :

- `com.example.authstarter.auth`: login, logout, session payloads, password change, invitation acceptance et password reset
- `com.example.authstarter.config.security`: Spring Security, CORS, CSRF et GraphQL authorization
- `com.example.authstarter.foundation`: current user, organization, workspace et membership context
- `com.example.authstarter.admin`: admin user management
- `com.example.authstarter.notification`: notification events et email provider selection
- `backend/src/main/resources/db/migration`: Flyway migrations
- `backend/src/main/resources/graphql/schema.graphqls`: GraphQL schema

Le backend utilise JDBC et Flyway avec PostgreSQL. Les local migrations créent workspaces, organizations, users, memberships, user credentials, security tokens et notification events.

## Frontend

Le frontend est une Angular application dans [../../frontend](../../frontend). Zones importantes :

- `src/app/core/auth`: session bootstrap, login/logout, CSRF fetch et password flows
- `src/app/core/guards`: app-shell, guest-shell et role access guards
- `src/app/core/graphql`: Apollo Angular client configuration
- `src/app/core/runtime-config`: optional runtime configuration loading
- `src/app/features/auth`: login, invite acceptance, forgot password et reset password pages
- `src/app/features/admin`: admin user management page
- `src/app/features/notifications`: notification history page

Les GraphQL requests du frontend utilisent `withCredentials: true`, donc le navigateur envoie le backend session cookie.

## Request Flow

1. Le frontend appelle `GET /auth/csrf`.
2. Le backend renvoie un CSRF payload et utilise aussi le cookie `XSRF-TOKEN`.
3. Les unsafe frontend requests ajoutent le header `X-XSRF-TOKEN`.
4. Login appelle `login` sur `POST /graphql`.
5. Le backend stocke le Spring Security context dans la server-side session.
6. Les authenticated GraphQL operations utilisent le cookie `AUTH_STARTER_SESSION`.

## Roles

Le role enum courant est :

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Les admin routes et GraphQL operations demandent `SUPERADMIN` ou `ORG_ADMIN` ; seul un `SUPERADMIN` existant peut attribuer `SUPERADMIN`.
