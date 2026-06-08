[Documentation index](./README.md)

# Getting Started

## Prerequisites

- Java 21
- Node 22.14.0
- pnpm 10.6.5, or `npx -y pnpm@10.6.5`
- Docker Compose for the local PostgreSQL/backend path

## Run with Docker Compose

From the repository root:

```sh
docker compose up --build
```

This starts:

- `db` on `127.0.0.1:5432`
- `backend` on `127.0.0.1:8080`

The Compose file sets local backend variables directly and is local-only. Use [../../.env.example](../../.env.example) for safe defaults. Use [../../.env.local.example](../../.env.local.example) only when you need local demo credentials.

## Run the Frontend

In a second terminal:

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

Open `http://localhost:4200`.

The frontend development config uses:

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

## Run the Backend Without Docker

Start PostgreSQL locally with database, username, and password set to `authstarter`, then run:

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

The default datasource is `jdbc:postgresql://localhost:5432/authstarter`.

## Local Login

These credentials are local-only demo credentials. They are enabled by the `local` profile and Docker Compose, and must not be used in deployed environments.

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

Break-glass authentication is disabled by default in `application.yml`; local config explicitly enables it for these demo users.

GraphiQL and GraphQL introspection are enabled by local/dev configuration for developer use. Base configuration keeps GraphiQL and introspection disabled.

## Useful Checks

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

The repository root also has:

```sh
make verify
```
