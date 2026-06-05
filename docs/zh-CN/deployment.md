[文档首页](./README.md)

# Deployment

本 repository 包含 local development Compose setup 和 backend Dockerfile。当前不包含 production infrastructure，也不包含单独的 frontend container image。

## Backend Image

[../../backend/Dockerfile](../../backend/Dockerfile) 使用 Java 21 构建 Spring Boot jar，然后在 Java 21 JRE image 中运行。Container 暴露 port `8080`。

Entrypoint 运行：

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}
```

Local development 之外应显式设置 `SPRING_PROFILES_ACTIVE`。

## Backend Runtime Configuration

至少配置：

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE`
- `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`

使用 SMTP delivery 时，还要配置：

- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`
- `AUTH_STARTER_SMTP_USERNAME`
- `AUTH_STARTER_SMTP_PASSWORD`
- `AUTH_STARTER_SMTP_AUTH`
- `AUTH_STARTER_SMTP_START_TLS`

部署前查看 [../../.env.example](../../.env.example) 和 [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml)。

## Frontend Build

构建 Angular app：

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 build
```

Development environment 当前指向 `http://localhost:8080`。`RuntimeConfigService` 已有 runtime config support，[../../frontend/public/config.template.json](../../frontend/public/config.template.json) 展示 expected shape，但需要 environment file 设置 `runtimeConfigPath` 后 app 才会加载 remote runtime config。

## Local Compose Scope

[../../docker-compose.yml](../../docker-compose.yml) 用于 local development，启动 PostgreSQL 和 backend。它不是 production deployment template。

部署到真实系统前，请检查 cookie security、HTTPS、CORS origins、SMTP、password policy、monitoring、backup、retention 和 operational access。
