[文档首页](./README.md)

# Authentication

## Model

Authentication 使用 session-based model。Backend 创建 server-side session，并发送 `AUTH_STARTER_SESSION` cookie。Angular frontend 对 backend requests 启用 credentials。

CSRF protection 已启用。Frontend 从以下 endpoint 初始化 token：

- `GET /auth/csrf`

Unsafe requests 使用：

- `X-XSRF-TOKEN`

## Public Operations

Backend 允许这些 GraphQL root fields 在未认证时访问：

- Query: `readiness`, `currentSession`
- Mutation: `login`, `logout`, `acceptUserInvite`, `requestPasswordReset`, `resetPassword`

其他 GraphQL operations 需要 authenticated session。

## Login Sources

`BaselineAuthService` 会先尝试 DB-backed credentials（当 `UserCredentialAuthenticationService` 可用时）。如果失败或没有匹配的 persisted credential，并且 `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`，configured break-glass users 可以登录。

本地 configured users 来自：

- [application.yml](../../backend/src/main/resources/application.yml)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)

## RBAC

Roles 由 `AuthStarterRole` 表示：

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Angular admin route 使用 `roleAccessGuard`。Backend admin management 检查 authenticated principal 和 current organization context。

## Invitation and Password Reset

Invitation 和 password reset tokens 以 hash 形式存储在 `user_security_tokens`。Admin user creation 会调用 invitation service 和 notification service。Password reset request 返回 generic success message，避免暴露 account 是否存在。

Email delivery 通过以下变量选择：

- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=smtp`

## Important Endpoints

- `POST /graphql`
- `GET /auth/csrf`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
