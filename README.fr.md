[English](./README.md) | [简体中文](./README.zh-CN.md) | [Français](./README.fr.md)

# Spring Boot Angular Auth Starter

> Ce fichier est la traduction française de [README.md](./README.md) ; le README anglais reste la référence officielle de la documentation.

Ce projet est un starter open source réutilisable avec un backend d'authentification Spring Boot et un frontend d'authentification Angular. Le modèle d'intégration par défaut utilise des sessions côté serveur, une protection CSRF, une persistance PostgreSQL, GraphQL et un contrôle d'accès par rôles.

## Objectif

Le projet fournit une base pratique pour l'authentification, l'autorisation, la gestion du cycle de vie des utilisateurs, les invitations, la configuration et la réinitialisation des mots de passe, ainsi que l'historique des notifications de compte. C'est un starter, pas une plateforme d'identité complète déjà déployée.

## Fonctionnalités

- Backend Spring Boot avec Spring Security, Spring GraphQL, JDBC, Flyway et PostgreSQL
- Frontend Angular avec login, logout, session bootstrap, route guards, account page, user management et notification history
- Authentification par session côté serveur avec le cookie `AUTH_STARTER_SESSION`
- Initialisation CSRF via `GET /auth/csrf` et header `X-XSRF-TOKEN` pour les requêtes non sûres
- Rôles RBAC : `SUPERADMIN`, `ORG_ADMIN`, `USER`
- Requêtes de current user, organization context, workspace et membership
- Création et mise à jour d'admin users par `SUPERADMIN` et `ORG_ADMIN`
- Invitation flow et first-login password setup avec des tokens à usage unique hashés
- Flux forgot password et password reset
- Historique des notification events avec les fournisseurs email `local-mock` ou `smtp`
- Configuration Docker Compose locale pour PostgreSQL et le backend

## Stack Technique

- Backend : Java 21, Spring Boot 3.5.14, Spring Security, Spring GraphQL, JDBC, Flyway
- Base de données : PostgreSQL 16 dans Docker Compose local
- Frontend : Angular 20.2, Apollo Angular, RxJS, lucide-angular
- Outils : Gradle wrapper, Node 22.14.0, pnpm 10.6.5, Docker Compose

## Installation Locale

Démarrer PostgreSQL et le backend :

```sh
docker compose up --build
```

Le backend écoute sur `http://localhost:8080`. [docker-compose.yml](./docker-compose.yml) lie les ports backend et PostgreSQL à `127.0.0.1` et sert uniquement au développement local. [.env.example](./.env.example) contient des valeurs sûres par défaut ; [.env.local.example](./.env.local.example) contient les identifiants de démonstration local-only.

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

Ces identifiants sont des identifiants de démonstration local-only fournis par les profils `local`/`dev` et Docker Compose. Ne pas les utiliser dans un environnement déployé.

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

L'authentification break-glass est désactivée par défaut dans [application.yml](./backend/src/main/resources/application.yml). Le profil local et Docker Compose l'activent explicitement uniquement pour la démonstration locale.

Les mutations publiques d'authentification utilisent un rate limiter basique en mémoire. En production ou avec plusieurs instances, le remplacer ou l'appuyer sur un stockage distribué comme Redis.

GraphiQL est désactivé par défaut. L'introspection GraphQL est désactivée par défaut avec `AUTH_STARTER_GRAPHQL_INTROSPECTION_ENABLED=false` ; la configuration local/dev l'active pour le développement.

## Aperçu API

- Point d'entrée GraphQL : `POST /graphql`
- Point d'entrée CSRF : `GET /auth/csrf`
- Points d'entrée de santé : `GET /actuator/health`, `GET /actuator/health/liveness`, `GET /actuator/health/readiness`

Opérations GraphQL publiques :

- `readiness`
- `currentSession`
- `login`
- `logout`
- `acceptUserInvite`
- `requestPasswordReset`
- `resetPassword`

Opérations nécessitant une authentification :

- `currentUserProfile`
- `currentOrganizationContext`
- `foundationOrganizations`
- `rbacBaseline`
- `notificationEvents`
- `changeOwnPassword`

Opérations d'administration nécessitant `SUPERADMIN` ou `ORG_ADMIN` :

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

Ou depuis la racine du dépôt :

```sh
make verify
```

## Documentation

- [Démarrage](./docs/fr/getting-started.md)
- [Architecture](./docs/fr/architecture.md)
- [Authentification](./docs/fr/authentication.md)
- [Déploiement](./docs/fr/deployment.md)
- [Dépannage](./docs/fr/troubleshooting.md)
- [Maintenance de la documentation](./docs/documentation-maintenance.md)

## License

MIT. See [LICENSE](./LICENSE).
