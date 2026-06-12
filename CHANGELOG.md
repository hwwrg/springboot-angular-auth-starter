# Changelog

## Unreleased

- No unreleased changes yet.

## v0.2.0 - 2026-06-12

Authentication options and end-to-end test coverage.

- Optional OAuth2/OIDC login (Google, GitHub, enterprise OIDC), linking a verified provider email to an existing active account without provisioning new accounts.
- Optional TOTP multi-factor authentication with single-use, hashed recovery codes; MFA-enabled accounts complete a second-factor challenge after a correct password.
- Playwright end-to-end coverage for the forgot-password and reset-password flows.
- Continuous integration workflow that runs the end-to-end suite against the Docker Compose stack.
- Mapped the public-auth rate-limit error to a dedicated GraphQL error type instead of an opaque internal error.

## v0.1.0 - 2026-06-05

Initial open source release.

- Extracted `springboot-angular-auth-starter` as a generic open-source authentication starter.
- Removed application-specific modules, deployment artifacts, and environment-specific documentation.
- Kept reusable authentication, RBAC, user management, invitation, password reset, and local notification foundations.
- Spring Boot backend with session-based authentication, CSRF protection, and role-based access control.
- Angular frontend with login, logout, account management, user management, and notification history.
- User invitation flow with first-login password setup.
- Forgot-password and password-reset flows.
- Notification framework with `local-mock` and `smtp` email providers.
- Local Docker Compose setup for PostgreSQL and the backend.
