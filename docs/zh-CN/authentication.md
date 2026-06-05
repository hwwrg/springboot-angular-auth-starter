[文档首页](./README.md)

# 认证

## 模型

认证采用基于 session 的模型。后端创建服务端 session，并发送 `AUTH_STARTER_SESSION` cookie。Angular 前端发送后端请求时会携带凭据。

CSRF 防护已启用。前端通过以下端点初始化 token：

- `GET /auth/csrf`

非安全请求使用：

- `X-XSRF-TOKEN`

## 公开操作

后端允许这些 GraphQL root fields 在未认证时访问：

- Query: `readiness`, `currentSession`
- Mutation: `login`, `logout`, `acceptUserInvite`, `requestPasswordReset`, `resetPassword`

其他 GraphQL 操作需要已认证的 session。

## 登录来源

当 `UserCredentialAuthenticationService` 可用时，`BaselineAuthService` 会先尝试数据库凭据。如果没有匹配的持久化凭据，或者凭据校验失败，并且 `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`，已配置的 break-glass users 可以登录。

本地配置用户来自：

- [application.yml](../../backend/src/main/resources/application.yml)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)

## RBAC

Roles 由 `AuthStarterRole` 表示：

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Angular admin route 使用 `roleAccessGuard`。后端 admin management 会检查已认证的 principal 和 current organization context。

## Invitation and Password Reset

Invitation 和 password reset tokens 以哈希形式存储在 `user_security_tokens`。Admin user creation 会调用 invitation service 和 notification service。Password reset request 返回通用成功消息，避免暴露账号是否存在。

邮件投递通过以下变量选择：

- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=smtp`

## 重要端点

- `POST /graphql`
- `GET /auth/csrf`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
