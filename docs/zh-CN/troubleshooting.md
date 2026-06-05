[文档首页](./README.md)

# Troubleshooting

## Backend Cannot Connect to PostgreSQL

确认 PostgreSQL 正在运行，并且 datasource variables 与数据库一致：

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`

不使用 Docker 运行 backend 时，默认 URL 是 `jdbc:postgresql://localhost:5432/authstarter`。

## Frontend Cannot Reach Backend

确认 backend 在 `http://localhost:8080`，并且 frontend config 指向：

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

同时确认 local CORS 使用 `AUTH_STARTER_FRONTEND_ORIGIN=http://localhost:4200`。

## CSRF or 403 Errors

Frontend 应在 unsafe GraphQL requests 前调用 `GET /auth/csrf`。Unsafe requests 必须包含 `X-XSRF-TOKEN`，browser requests 必须 include credentials。

如果本地 cookie 没有发送，检查：

- `AUTH_STARTER_SESSION_COOKIE_SECURE=false`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE=lax`
- frontend and backend origins

## Login Fails

本地开发先尝试：

- `operator@authstarter.local`
- `authstarter-local-password`

如果 DB-backed credentials 被修改，configured break-glass user 只有在 `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true` 时可用。

## Admin Page Is Not Accessible

Frontend admin route 和 backend admin operations 需要 `SUPERADMIN` 或 `ORG_ADMIN`。`USER` role 会被重定向到 not-authorized page。

## Email Is Not Delivered

`AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock` 会记录 notification events，但不会发送真实 email。要使用 local mail catcher 或真实 SMTP service，请设置 `smtp` 并配置 `AUTH_STARTER_SMTP_*` variables。

## Port Conflicts

Local setup 预期：

- frontend: `4200`
- backend: `8080`
- PostgreSQL: `5432`

停止冲突服务，或修改相关 local command/configuration。
