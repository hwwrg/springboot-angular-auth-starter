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

`BaselineAuthService` first tries DB-backed credentials when `UserCredentialAuthenticationService` is available. If that fails or no persisted credential matches, configured break-glass users can authenticate when `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

Local configured users come from:

- [application.yml](../../backend/src/main/resources/application.yml)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)

## RBAC

Roles are represented by `AuthStarterRole`:

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

The Angular admin route uses `roleAccessGuard`. Backend admin management checks the authenticated principal and current organization context.

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
