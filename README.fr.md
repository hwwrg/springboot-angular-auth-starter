[English](./README.md) | [简体中文](./README.zh-CN.md) | [Français](./README.fr.md)

# Spring Boot Angular Auth Starter

> This file is a French translation of [README.md](./README.md). The English README is the source of truth.

Ce projet est un starter open source réutilisable avec un Spring Boot authentication backend et un Angular authentication frontend. Le modèle d'intégration par défaut utilise server-side sessions, CSRF protection, PostgreSQL persistence, GraphQL et role-based access control.

## Objectif

Le projet fournit une base pratique pour authentication, authorization, user lifecycle management, invitations, password setup/reset et account notification history. C'est un starter, pas une identity platform complète prête pour production.

## Fonctionnalités

- Spring Boot backend avec Spring Security, Spring GraphQL, JDBC, Flyway et PostgreSQL
- Angular frontend avec login, logout, session bootstrap, route guards, account page, user management et notification history
- Server-side session authentication avec le cookie `AUTH_STARTER_SESSION`
- CSRF bootstrap via `GET /auth/csrf` et header `X-XSRF-TOKEN` pour les unsafe requests
- RBAC roles : `SUPERADMIN`, `ORG_ADMIN`, `USER`
- Current user, organization context, workspace et membership queries
- Admin user creation/update pour `SUPERADMIN` et `ORG_ADMIN`
- Invitation flow et first-login password setup avec hashed single-use tokens
- Forgot password et password reset flows
- Notification event history avec providers email `local-mock` ou `smtp`
- Local Docker Compose setup pour PostgreSQL et le backend

## Stack Technique

- Backend : Java 21, Spring Boot 3.5.10, Spring Security, Spring GraphQL, JDBC, Flyway
- Database : PostgreSQL 16 dans local Docker Compose
- Frontend : Angular 20.2, Apollo Angular, RxJS, lucide-angular
- Tooling : Gradle wrapper, Node 22.14.0, pnpm 10.6.5, Docker Compose

## Installation Locale

Démarrer PostgreSQL et le backend :

```sh
docker compose up --build
```

Le backend écoute sur `http://localhost:8080`. Le fichier [docker-compose.yml](./docker-compose.yml) fournit directement les local backend environment values ; [.env.example](./.env.example) sert de référence pour les variables configurables.

Démarrer le frontend séparément :

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

Ouvrir `http://localhost:4200`.

Backend sans Docker :

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

Par défaut, PostgreSQL doit être disponible sur `jdbc:postgresql://localhost:5432/authstarter`, sauf si `AUTH_STARTER_DATASOURCE_URL` et les variables associées sont définies.

## Utilisateurs Locaux Par Défaut

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

## Aperçu API

- GraphQL endpoint : `POST /graphql`
- CSRF endpoint : `GET /auth/csrf`
- Health endpoints : `GET /actuator/health`, `GET /actuator/health/liveness`, `GET /actuator/health/readiness`

Public GraphQL operations :

- `readiness`
- `currentSession`
- `login`
- `logout`
- `acceptUserInvite`
- `requestPasswordReset`
- `resetPassword`

Authenticated operations :

- `currentUserProfile`
- `currentOrganizationContext`
- `foundationOrganizations`
- `rbacBaseline`
- `notificationEvents`
- `changeOwnPassword`

Admin operations nécessitant `SUPERADMIN` ou `ORG_ADMIN` :

- `adminManagementBaseline`
- `adminCreateUser`
- `adminUpdateUser`

Le schema courant est dans [schema.graphqls](./backend/src/main/resources/graphql/schema.graphqls).

## Vérification

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

Ou depuis la racine du repository :

```sh
make verify
```

## Documentation

- [Démarrage](./docs/fr/getting-started.md)
- [Architecture](./docs/fr/architecture.md)
- [Authentication](./docs/fr/authentication.md)
- [Deployment](./docs/fr/deployment.md)
- [Troubleshooting](./docs/fr/troubleshooting.md)
- [Documentation maintenance](./docs/documentation-maintenance.md)

## License

MIT. See [LICENSE](./LICENSE).
