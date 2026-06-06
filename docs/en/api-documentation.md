# API Documentation

The backend exposes interactive OpenAPI documentation for REST support endpoints and keeps the GraphQL schema as the source of truth for authentication, RBAC, user-management, invitation, password-reset, and notification operations.

## Local URLs

Start the backend with the local profile:

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

Then open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- GraphQL endpoint: `http://localhost:8080/graphql`
- GraphQL schema: [`schema.graphqls`](../../backend/src/main/resources/graphql/schema.graphqls)

Swagger UI is useful for browser-driven exploration of REST support endpoints such as `GET /auth/csrf` and operational health endpoints. GraphQL operations are documented through the schema and examples below.

## Browser Client Flow

1. Fetch a CSRF token from `GET /auth/csrf`.
2. Send GraphQL operations to `POST /graphql`.
3. Include the returned CSRF token in the `X-XSRF-TOKEN` header for unsafe requests.
4. Keep credentials enabled so the browser sends the `AUTH_STARTER_SESSION` cookie after login.

## Authentication Example

```graphql
mutation Login($input: LoginInput!) {
  login(input: $input) {
    authenticated
    mustChangePassword
    principal {
      id
      email
      displayName
      roles
      mustChangePassword
    }
  }
}
```

Variables:

```json
{
  "input": {
    "email": "operator@authstarter.local",
    "password": "authstarter-local-password"
  }
}
```

## Invitation Example

```graphql
mutation AcceptUserInvite($input: AcceptUserInviteInput!) {
  acceptUserInvite(input: $input) {
    userId
    email
    status
  }
}
```

Variables:

```json
{
  "input": {
    "token": "single-use-invitation-token",
    "newPassword": "replace-with-a-strong-password"
  }
}
```

## Password Reset Examples

Request a password reset:

```graphql
mutation RequestPasswordReset($input: PasswordResetRequestInput!) {
  requestPasswordReset(input: $input) {
    message
  }
}
```

Complete a password reset:

```graphql
mutation ResetPassword($input: PasswordResetCompleteInput!) {
  resetPassword(input: $input) {
    message
  }
}
```

Variables:

```json
{
  "input": {
    "token": "single-use-password-reset-token",
    "newPassword": "replace-with-a-strong-password"
  }
}
```

## User Management Examples

Create a user as a `SUPERADMIN` or `ORG_ADMIN`:

```graphql
mutation AdminCreateUser($input: CreateAdminUserInput!) {
  adminCreateUser(input: $input) {
    id
    email
    displayName
    status
    role
    membershipStatus
    primaryMembership
  }
}
```

List the current admin management baseline:

```graphql
query AdminManagementBaseline {
  adminManagementBaseline {
    users {
      id
      email
      displayName
      role
      status
    }
    totals {
      userCount
      notificationEventCount
    }
  }
}
```
