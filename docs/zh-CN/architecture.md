[文档首页](./README.md)

# 架构

## 后端

后端是 [../../backend](../../backend) 下的 Spring Boot 应用。主要区域：

- `com.example.authstarter.auth`: login、logout、session payloads、password change、invitation acceptance、password reset
- `com.example.authstarter.config.security`: Spring Security、CORS、CSRF、GraphQL authorization
- `com.example.authstarter.foundation`: current user、organization、workspace、membership context
- `com.example.authstarter.admin`: admin user management
- `com.example.authstarter.notification`: notification events 和 email provider 选择
- `backend/src/main/resources/db/migration`: Flyway migrations
- `backend/src/main/resources/graphql/schema.graphqls`: GraphQL schema

后端通过 JDBC 和 Flyway 使用 PostgreSQL。本地 migrations 会创建 workspaces、organizations、users、memberships、user credentials、security tokens 和 notification events。

## 前端

前端是 [../../frontend](../../frontend) 下的 Angular 应用。主要区域：

- `src/app/core/auth`: session bootstrap、login/logout、CSRF fetch、password flows
- `src/app/core/guards`: app-shell、guest-shell、role access guards
- `src/app/core/graphql`: Apollo Angular client configuration
- `src/app/core/runtime-config`: 可选的 runtime configuration 加载
- `src/app/features/auth`: login、invite acceptance、forgot password、reset password pages
- `src/app/features/admin`: admin user management page
- `src/app/features/notifications`: notification history page

前端 GraphQL 请求使用 `withCredentials: true`，因此浏览器会发送后端 session cookie。

## 请求流程

1. 前端调用 `GET /auth/csrf`。
2. 后端返回 CSRF payload，并使用 `XSRF-TOKEN` cookie。
3. 前端的非安全请求带上 `X-XSRF-TOKEN` 请求头。
4. Login 通过 `POST /graphql` 调用 `login`。
5. 后端将 Spring Security context 存入服务端 session。
6. 需要认证的 GraphQL 操作使用 `AUTH_STARTER_SESSION` cookie。

## 角色

当前 role enum：

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Admin routes 和 GraphQL operations 需要 `SUPERADMIN` 或 `ORG_ADMIN`；只有已有 `SUPERADMIN` 可以分配 `SUPERADMIN`。
