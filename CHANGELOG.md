# Changelog

## Unreleased

- No unreleased changes yet.

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
