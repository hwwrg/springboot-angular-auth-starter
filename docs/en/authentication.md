[Documentation index](./README.md)

# Authentication

## Model

Authentication is session-based. The backend creates a server-side session and sends the `AUTH_STARTER_SESSION` cookie. The Angular frontend sends backend requests with credentials enabled.

CSRF protection is enabled. The frontend bootstraps a token from:

- `GET /auth/csrf`

Unsafe requests use:

- `X-XSRF-TOKEN`

## Public Operations

The backend permits these GraphQL root fields without an authenticated session:

- Query: `readiness`, `currentSession`
- Mutation: `login`, `logout`, `acceptUserInvite`, `requestPasswordReset`, `resetPassword`

Other GraphQL operations require an authenticated session.

## Login Sources

`BaselineAuthService` first tries DB-backed credentials when `UserCredentialAuthenticationService` is available. If that fails or no persisted credential matches, configured break-glass users can authenticate only when `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

Break-glass authentication is disabled by default in [application.yml](../../backend/src/main/resources/application.yml), and that file does not provide production-capable baseline username or password defaults. Local demo credentials are defined only in local development configuration:

- [application-local.yml](../../backend/src/main/resources/application-local.yml)
- [application-dev.yml](../../backend/src/main/resources/application-dev.yml)
- [docker-compose.yml](../../docker-compose.yml)

Do not use the local demo credentials in deployed environments.

## Rate Limiting

Public auth mutations use a basic in-memory fixed-window rate limiter:

- `login`
- `requestPasswordReset`
- `resetPassword`
- `acceptUserInvite`

This is starter-friendly protection for a single backend process. Production and multi-instance deployments should replace or back it with distributed storage such as Redis.

## GraphQL Abuse Protection

GraphQL requests have configurable depth, complexity, and request body size limits:

- `AUTH_STARTER_GRAPHQL_MAX_QUERY_DEPTH`
- `AUTH_STARTER_GRAPHQL_MAX_QUERY_COMPLEXITY`
- `AUTH_STARTER_GRAPHQL_MAX_REQUEST_BYTES`

GraphiQL is disabled by default. GraphQL introspection is controlled with `AUTH_STARTER_GRAPHQL_INTROSPECTION_ENABLED`; base configuration disables introspection, while local/dev configuration enables it for developer use.

## RBAC

Roles are represented by `AuthStarterRole`:

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

The Angular admin route uses `roleAccessGuard`. Backend admin management checks the authenticated principal and current organization context. Only `SUPERADMIN` can assign or modify `SUPERADMIN`; `ORG_ADMIN` cannot modify `SUPERADMIN` users, cannot change its own role/status, and cannot assign `ORG_ADMIN` unless the backend policy explicitly allows it.

## Invitation and Password Reset

Invitation and password reset tokens are stored hashed in `user_security_tokens`. Admin user creation calls the invitation service and notification service. Password reset requests return a generic success message so account existence is not exposed.

Email delivery is selected with:

- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=smtp`

## Important Endpoints

- `POST /graphql`
- `GET /auth/csrf`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
