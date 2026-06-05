[文档首页](./README.md)

# 架构

## Backend

Backend 是 [../../backend](../../backend) 下的 Spring Boot application。主要区域：

- `com.example.authstarter.auth`: login、logout、session payloads、password change、invitation acceptance、password reset
- `com.example.authstarter.config.security`: Spring Security、CORS、CSRF、GraphQL authorization
- `com.example.authstarter.foundation`: current user、organization、workspace、membership context
- `com.example.authstarter.admin`: admin user management
- `com.example.authstarter.notification`: notification events 和 email provider selection
- `backend/src/main/resources/db/migration`: Flyway migrations
- `backend/src/main/resources/graphql/schema.graphqls`: GraphQL schema

Backend 使用 JDBC 和 Flyway 访问 PostgreSQL。Local migrations 创建 workspaces、organizations、users、memberships、user credentials、security tokens 和 notification events。

## Frontend

Frontend 是 [../../frontend](../../frontend) 下的 Angular application。主要区域：

- `src/app/core/auth`: session bootstrap、login/logout、CSRF fetch、password flows
- `src/app/core/guards`: app-shell、guest-shell、role access guards
- `src/app/core/graphql`: Apollo Angular client configuration
- `src/app/core/runtime-config`: optional runtime configuration loading
- `src/app/features/auth`: login、invite acceptance、forgot password、reset password pages
- `src/app/features/admin`: admin user management page
- `src/app/features/notifications`: notification history page

Frontend 的 GraphQL requests 使用 `withCredentials: true`，因此浏览器会发送 backend session cookie。

## Request Flow

1. Frontend 调用 `GET /auth/csrf`。
2. Backend 返回 CSRF payload，并使用 `XSRF-TOKEN` cookie。
3. Unsafe frontend requests 带上 `X-XSRF-TOKEN` header。
4. Login 调用 `POST /graphql` 上的 `login`。
5. Backend 将 Spring Security context 存入 server-side session。
6. Authenticated GraphQL operations 使用 `AUTH_STARTER_SESSION` cookie。

## Roles

当前 role enum：

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

Admin routes 和 GraphQL operations 需要 `SUPERADMIN` 或 `ORG_ADMIN`；只有已有 `SUPERADMIN` 可以分配 `SUPERADMIN`。
