[English](./README.md) | [简体中文](./README.zh-CN.md) | [Français](./README.fr.md)

# Spring Boot Angular Auth Starter

> 本文件是 [README.md](./README.md) 的简体中文译本；英文 README 是本项目文档的权威来源。

这是一个可复用的开源 starter，包含 Spring Boot 认证后端和 Angular 认证前端。默认集成方式使用服务端 session、CSRF 防护、PostgreSQL 持久化、GraphQL 和基于角色的访问控制。

## 用途

本项目为认证、授权、用户生命周期管理、邀请、密码设置与重置，以及账号通知历史提供实用基础。它是一个 starter，不是完整的已部署身份平台。

## 功能

- Spring Boot 后端：Spring Security、Spring GraphQL、JDBC、Flyway、PostgreSQL
- Angular 前端：login、logout、session bootstrap、route guards、account page、user management、notification history
- 使用 `AUTH_STARTER_SESSION` cookie 的服务端 session 认证
- 通过 `GET /auth/csrf` 初始化 CSRF token，非安全请求使用 `X-XSRF-TOKEN`
- RBAC 角色：`SUPERADMIN`、`ORG_ADMIN`、`USER`
- Current user、organization context、workspace 和 membership queries
- `SUPERADMIN` 和 `ORG_ADMIN` 可创建和更新 admin users
- 使用哈希化一次性 token 的 invitation flow 和 first-login password setup
- Forgot password 和 password reset 流程
- 使用 `local-mock` 或 `smtp` email provider 的 notification event history
- PostgreSQL 和后端的本地 Docker Compose 配置

## 技术栈

- 后端：Java 21、Spring Boot 3.5.10、Spring Security、Spring GraphQL、JDBC、Flyway
- 数据库：本地 Docker Compose 中的 PostgreSQL 16
- 前端：Angular 20.2、Apollo Angular、RxJS、lucide-angular
- 工具：Gradle wrapper、Node 22.14.0、pnpm 10.6.5、Docker Compose

## 本地运行

启动 PostgreSQL 和后端：

```sh
docker compose up --build
```

后端地址是 `http://localhost:8080`。当前 [docker-compose.yml](./docker-compose.yml) 已直接提供本地后端环境变量；[.env.example](./.env.example) 是可配置变量参考。

单独启动前端：

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

打开 `http://localhost:4200`。

不使用 Docker 运行后端：

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

默认需要 PostgreSQL 位于 `jdbc:postgresql://localhost:5432/authstarter`，除非设置 `AUTH_STARTER_DATASOURCE_URL` 等相关变量。

## 默认本地用户

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

## API 概览

- GraphQL 端点：`POST /graphql`
- CSRF 端点：`GET /auth/csrf`
- 健康检查端点：`GET /actuator/health`、`GET /actuator/health/liveness`、`GET /actuator/health/readiness`

公开 GraphQL 操作：

- `readiness`
- `currentSession`
- `login`
- `logout`
- `acceptUserInvite`
- `requestPasswordReset`
- `resetPassword`

需要认证的操作：

- `currentUserProfile`
- `currentOrganizationContext`
- `foundationOrganizations`
- `rbacBaseline`
- `notificationEvents`
- `changeOwnPassword`

需要 `SUPERADMIN` 或 `ORG_ADMIN` 的管理员操作：

- `adminManagementBaseline`
- `adminCreateUser`
- `adminUpdateUser`

当前 schema 见 [schema.graphqls](./backend/src/main/resources/graphql/schema.graphqls)。

## 验证

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

也可以在仓库根目录运行：

```sh
make verify
```

## 文档

- [入门](./docs/zh-CN/getting-started.md)
- [架构](./docs/zh-CN/architecture.md)
- [认证](./docs/zh-CN/authentication.md)
- [部署](./docs/zh-CN/deployment.md)
- [故障排查](./docs/zh-CN/troubleshooting.md)
- [文档维护](./docs/documentation-maintenance.md)

## License

MIT. See [LICENSE](./LICENSE).
