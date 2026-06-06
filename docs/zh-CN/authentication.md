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

当 `UserCredentialAuthenticationService` 可用时，`BaselineAuthService` 会先尝试数据库凭据。如果没有匹配的持久化凭据，或者凭据校验失败，已配置的 break-glass users 只有在 `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true` 时才能登录。

Break-glass 认证在 [application.yml](../../backend/src/main/resources/application.yml) 中默认关闭，而且该文件不提供可用于生产的 baseline username 或 password 默认值。Local-only 演示凭据只定义在本地开发配置中：

- [application-local.yml](../../backend/src/main/resources/application-local.yml)
- [application-dev.yml](../../backend/src/main/resources/application-dev.yml)
- [docker-compose.yml](../../docker-compose.yml)

不要在部署环境中使用这些本地演示凭据。

## Rate Limiting

公开认证 mutations 使用基础的内存固定窗口 rate limiter：

- `login`
- `requestPasswordReset`
- `resetPassword`
- `acceptUserInvite`

这是适合单个 backend 进程的 starter 级保护。生产或多实例部署应替换为或接入 Redis 等分布式存储。

## GraphQL Abuse Protection

GraphQL 请求有可配置的深度、复杂度和请求体大小限制：

- `AUTH_STARTER_GRAPHQL_MAX_QUERY_DEPTH`
- `AUTH_STARTER_GRAPHQL_MAX_QUERY_COMPLEXITY`
- `AUTH_STARTER_GRAPHQL_MAX_REQUEST_BYTES`

GraphiQL 默认关闭。GraphQL introspection 由 `AUTH_STARTER_GRAPHQL_INTROSPECTION_ENABLED` 控制；基础配置关闭 introspection，local/dev 配置为开发启用它。

## RBAC

Roles 由 `AuthStarterRole` 表示：

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Angular admin route 使用 `roleAccessGuard`。后端 admin management 会检查已认证的 principal 和 current organization context。只有 `SUPERADMIN` 可以分配或修改 `SUPERADMIN`；`ORG_ADMIN` 不能修改 `SUPERADMIN` 用户，不能修改自己的 role、user status、membership status 或 primary membership，也不能分配 `ORG_ADMIN`，除非 backend policy 显式允许。

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
