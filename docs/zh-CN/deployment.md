[文档首页](./README.md)

# 部署

本仓库包含用于本地开发的 Compose 配置和后端 Dockerfile。当前不包含生产基础设施，也不包含单独的前端容器镜像。

## 后端镜像

[../../backend/Dockerfile](../../backend/Dockerfile) 使用 Java 21 构建 Spring Boot jar，然后在 Java 21 JRE image 中运行。容器暴露端口 `8080`。

运行时镜像使用非 root 用户运行，并且不安装 curl 或其他额外运行时工具。

入口命令运行：

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}
```

本地开发以外的环境应显式设置 `SPRING_PROFILES_ACTIVE`。

## 后端运行时配置

至少配置：

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE`
- `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`

使用 SMTP 邮件投递时，还要配置：

- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`
- `AUTH_STARTER_SMTP_USERNAME`
- `AUTH_STARTER_SMTP_PASSWORD`
- `AUTH_STARTER_SMTP_AUTH`
- `AUTH_STARTER_SMTP_START_TLS`

部署前查看 [../../.env.example](../../.env.example) 和 [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml)。[../../.env.local.example](../../.env.local.example) 是 local-only，不能用于部署环境。

Break-glass 认证默认关闭。不要部署 `application-local.yml`、`application-dev.yml`、`.env.local.example` 或 Docker Compose 中的本地演示凭据。

公开认证 mutations 包含基础的内存 rate limiter。生产或多实例部署应替换为或接入 Redis 等分布式存储。

部署 profile 应保持 GraphQL introspection 关闭，除非有明确运维需要。用 `AUTH_STARTER_GRAPHQL_*` 变量配置深度、复杂度和请求体大小限制。

## 前端构建

构建 Angular app：

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 build
```

开发环境当前指向 `http://localhost:8080`。`RuntimeConfigService` 已支持 runtime config，[../../frontend/public/config.template.json](../../frontend/public/config.template.json) 展示配置文件格式；但需要在环境文件中设置 `runtimeConfigPath` 后，应用才会加载远程 runtime config。

## 本地 Compose 范围

[../../docker-compose.yml](../../docker-compose.yml) 用于本地开发，启动 PostgreSQL 和后端。它不是生产部署模板。

Compose 的 PostgreSQL 和 backend 端口都为本地使用绑定到 `127.0.0.1`。

部署到真实系统前，请检查 cookie security、HTTPS、CORS origins、SMTP、password policy、monitoring、backup、retention 和 operational access。
