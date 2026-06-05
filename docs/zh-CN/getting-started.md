[文档首页](./README.md)

# 入门

## Prerequisites

- Java 21
- Node 22.14.0
- pnpm 10.6.5，或 `npx -y pnpm@10.6.5`
- 本地 PostgreSQL/backend 路径需要 Docker Compose

## 使用 Docker Compose 运行

在 repository root：

```sh
docker compose up --build
```

这会启动：

- `postgres`，host port `5432`
- `backend`，host port `8080`

当前 Compose 文件直接设置 local backend variables。[../../.env.example](../../.env.example) 是 configurable values 的参考。

## 运行 Frontend

在第二个 terminal：

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

打开 `http://localhost:4200`。

Frontend development config 使用：

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

## 不使用 Docker 运行 Backend

先启动本地 PostgreSQL，并将 database、username、password 都设为 `authstarter`，然后运行：

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

默认 datasource 是 `jdbc:postgresql://localhost:5432/authstarter`。

## 本地登录

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

## 常用检查

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

Repository root 也提供：

```sh
make verify
```
