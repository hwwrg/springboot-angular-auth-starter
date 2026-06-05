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

- `postgres` on host port `5432`
- `backend` on host port `8080`

The current Compose file sets the local backend variables directly. Use [../../.env.example](../../.env.example) as the reference for configurable values.

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

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

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
