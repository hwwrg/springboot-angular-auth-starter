[English](./README.md) | [简体中文](./README.zh-CN.md) | [Français](./README.fr.md)

# Spring Boot Angular Auth Starter

> This file is a Simplified Chinese translation of [README.md](./README.md). The English README is the source of truth.

这是一个可复用的开源 starter，包含 Spring Boot authentication backend 和 Angular authentication frontend。默认集成方式使用 server-side sessions、CSRF protection、PostgreSQL persistence、GraphQL 和 role-based access control。

## 用途

本项目提供 authentication、authorization、user lifecycle management、invitations、password setup/reset 和 account notification history 的实用基础。它是 starter，不是完整的已部署 identity platform。

## 功能

- Spring Boot backend：Spring Security、Spring GraphQL、JDBC、Flyway、PostgreSQL
- Angular frontend：login、logout、session bootstrap、route guards、account page、user management、notification history
- 使用 `AUTH_STARTER_SESSION` cookie 的 server-side session authentication
- `GET /auth/csrf` 获取 CSRF token，unsafe requests 使用 `X-XSRF-TOKEN`
- RBAC roles：`SUPERADMIN`、`ORG_ADMIN`、`USER`
- Current user、organization context、workspace 和 membership queries
- `SUPERADMIN` 和 `ORG_ADMIN` 可使用 admin user creation/update
- 使用 hashed single-use tokens 的 invitation 和 first-login password setup
- Forgot password 和 password reset flows
- 使用 `local-mock` 或 `smtp` email provider 的 notification event history
- PostgreSQL 和 backend 的 local Docker Compose setup

## 技术栈

- Backend：Java 21、Spring Boot 3.5.10、Spring Security、Spring GraphQL、JDBC、Flyway
- Database：local Docker Compose 中的 PostgreSQL 16
- Frontend：Angular 20.2、Apollo Angular、RxJS、lucide-angular
- Tooling：Gradle wrapper、Node 22.14.0、pnpm 10.6.5、Docker Compose

## 本地运行

启动 PostgreSQL 和 backend：

```sh
docker compose up --build
```

Backend 地址是 `http://localhost:8080`。当前 [docker-compose.yml](./docker-compose.yml) 已直接提供 local backend environment values；[.env.example](./.env.example) 是可配置变量参考。

单独启动 frontend：

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

打开 `http://localhost:4200`。

不使用 Docker 运行 backend：

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

默认需要 PostgreSQL 位于 `jdbc:postgresql://localhost:5432/authstarter`，除非设置 `AUTH_STARTER_DATASOURCE_URL` 等变量。

## 默认本地用户

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

## API 概览

- GraphQL endpoint：`POST /graphql`
- CSRF endpoint：`GET /auth/csrf`
- Health endpoints：`GET /actuator/health`、`GET /actuator/health/liveness`、`GET /actuator/health/readiness`

Public GraphQL operations：

- `readiness`
- `currentSession`
- `login`
- `logout`
- `acceptUserInvite`
- `requestPasswordReset`
- `resetPassword`

Authenticated operations：

- `currentUserProfile`
- `currentOrganizationContext`
- `foundationOrganizations`
- `rbacBaseline`
- `notificationEvents`
- `changeOwnPassword`

Admin operations 需要 `SUPERADMIN` 或 `ORG_ADMIN`：

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

或在 repository root 运行：

```sh
make verify
```

## 文档

- [入门](./docs/zh-CN/getting-started.md)
- [架构](./docs/zh-CN/architecture.md)
- [Authentication](./docs/zh-CN/authentication.md)
- [Deployment](./docs/zh-CN/deployment.md)
- [Troubleshooting](./docs/zh-CN/troubleshooting.md)
- [Documentation maintenance](./docs/documentation-maintenance.md)

## License

MIT. See [LICENSE](./LICENSE).
