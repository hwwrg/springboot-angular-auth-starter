[Documentation index](./README.md)

# Troubleshooting

## Backend Cannot Connect to PostgreSQL

Check that PostgreSQL is running and that the datasource variables match the running database:

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`

For the non-Docker backend path, the default URL is `jdbc:postgresql://localhost:5432/authstarter`.

## Frontend Cannot Reach Backend

Check that the backend is on `http://localhost:8080` and that the frontend config points to:

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

Also confirm `AUTH_STARTER_FRONTEND_ORIGIN=http://localhost:4200` for local CORS.

## CSRF or 403 Errors

The frontend should call `GET /auth/csrf` before unsafe GraphQL requests. Unsafe requests must include `X-XSRF-TOKEN`, and browser requests must include credentials.

If cookies are not sent locally, check:

- `AUTH_STARTER_SESSION_COOKIE_SECURE=false`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE=lax`
- frontend and backend origins

## Login Fails

For local development, try:

- `operator@authstarter.local`
- `authstarter-local-password`

These credentials are local-only and require the `local`/`dev` profile or Docker Compose. Break-glass authentication is disabled by default in `application.yml`; configured break-glass users work only when `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

If repeated public auth attempts fail with a rate-limit error, wait for the current window to expire. Production deployments should use a distributed limiter such as Redis instead of the built-in in-memory limiter.

## Admin Page Is Not Accessible

The frontend admin route and backend admin operations require `SUPERADMIN` or `ORG_ADMIN`. The `USER` role is redirected to the not-authorized page.

## Email Is Not Delivered

`AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock` records notification events but does not send real email. Use `smtp` and configure the `AUTH_STARTER_SMTP_*` variables for a local mail catcher or real SMTP service.

## Port Conflicts

The local setup expects:

- frontend: `4200`
- backend: `8080`
- PostgreSQL: `5432`

Stop conflicting services or change the relevant local command/configuration.
