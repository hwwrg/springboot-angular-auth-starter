[Index documentation](./README.md)

# Authentication

## Model

Authentication est session-based. Le backend crée une server-side session et envoie le cookie `AUTH_STARTER_SESSION`. L'Angular frontend envoie les backend requests avec credentials enabled.

CSRF protection est activée. Le frontend initialise le token via :

- `GET /auth/csrf`

Les unsafe requests utilisent :

- `X-XSRF-TOKEN`

## Public Operations

Le backend autorise ces GraphQL root fields sans authenticated session :

- Query : `readiness`, `currentSession`
- Mutation : `login`, `logout`, `acceptUserInvite`, `requestPasswordReset`, `resetPassword`

Les autres GraphQL operations demandent une authenticated session.

## Login Sources

`BaselineAuthService` essaie d'abord les DB-backed credentials lorsque `UserCredentialAuthenticationService` est disponible. Si cela échoue ou si aucun persisted credential ne correspond, les configured break-glass users peuvent s'authentifier quand `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

Les configured users locaux viennent de :

- [application.yml](../../backend/src/main/resources/application.yml)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)

## RBAC

Les roles sont représentés par `AuthStarterRole` :

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

L'Angular admin route utilise `roleAccessGuard`. Backend admin management vérifie l'authenticated principal et le current organization context.

## Invitation and Password Reset

Les invitation et password reset tokens sont stockés hashés dans `user_security_tokens`. Admin user creation appelle l'invitation service et la notification service. Password reset request renvoie un generic success message pour ne pas exposer l'existence d'un account.

Email delivery est sélectionné avec :

- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=smtp`

## Important Endpoints

- `POST /graphql`
- `GET /auth/csrf`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
