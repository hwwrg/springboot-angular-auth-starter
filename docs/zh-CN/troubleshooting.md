[文档首页](./README.md)

# 故障排查

## 后端无法连接 PostgreSQL

确认 PostgreSQL 正在运行，并且 datasource 变量与数据库一致：

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`

不使用 Docker 运行后端时，默认 URL 是 `jdbc:postgresql://localhost:5432/authstarter`。

## 前端无法访问后端

确认后端在 `http://localhost:8080`，并且前端配置指向：

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

同时确认本地 CORS 使用 `AUTH_STARTER_FRONTEND_ORIGIN=http://localhost:4200`。

## CSRF 或 403 错误

前端应在非安全 GraphQL 请求前调用 `GET /auth/csrf`。非安全请求必须包含 `X-XSRF-TOKEN`，浏览器请求必须携带凭据。

如果本地 cookie 没有发送，检查：

- `AUTH_STARTER_SESSION_COOKIE_SECURE=false`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE=lax`
- 前端和后端 origins

## 登录失败

本地开发先尝试：

- `operator@authstarter.local`
- `authstarter-local-password`

这些凭据是 local-only，并且需要 `local`/`dev` profile 或 Docker Compose。Break-glass 认证在 `application.yml` 中默认关闭；已配置的 break-glass users 只有在 `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true` 时可用。

如果重复公开认证请求因为 rate limit 失败，请等待当前窗口过期。生产部署应使用 Redis 等分布式 limiter，而不是内置内存 limiter。

如果 GraphQL 请求因为深度、复杂度、请求体大小或 introspection 错误失败，请检查当前 profile 的 `AUTH_STARTER_GRAPHQL_*` 变量。Local/dev 会为开发工具启用 introspection；基础部署配置会关闭它。

## 无法访问管理员页面

Frontend admin route 和 backend admin operations 需要 `SUPERADMIN` 或 `ORG_ADMIN`。`USER` role 会被重定向到 not-authorized page。

## 邮件未发送

`AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock` 会记录 notification events，但不会发送真实 email。要使用本地邮件捕获工具或真实 SMTP service，请设置 `smtp` 并配置 `AUTH_STARTER_SMTP_*` variables。

## 端口冲突

本地配置预期：

- frontend: `4200`
- backend: `8080`
- PostgreSQL: `5432`

停止冲突服务，或修改相关本地命令/配置。
