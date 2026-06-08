[Index de la documentation](./README.md)

# Démarrage

## Prérequis

- Java 21
- Node 22.14.0
- pnpm 10.6.5, ou `npx -y pnpm@10.6.5`
- Docker Compose pour le chemin local PostgreSQL/backend

## Lancer avec Docker Compose

Depuis la racine du dépôt :

```sh
docker compose up --build
```

Cela démarre :

- `db` sur `127.0.0.1:5432`
- `backend` sur `127.0.0.1:8080`

Le fichier Compose définit directement les variables locales du backend et est local-only. [../../.env.example](../../.env.example) fournit des valeurs sûres par défaut. Utiliser [../../.env.local.example](../../.env.local.example) uniquement pour les identifiants de démonstration locaux.

## Lancer le Frontend

Dans un second terminal :

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 start
```

Ouvrir `http://localhost:4200`.

La configuration frontend de développement utilise :

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

## Lancer le Backend sans Docker

Démarrer PostgreSQL localement avec database, username et password à `authstarter`, puis exécuter :

```sh
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

La datasource par défaut est `jdbc:postgresql://localhost:5432/authstarter`.

## Login Local

Ces identifiants sont des identifiants de démonstration local-only. Ils sont activés par le profil `local` et Docker Compose, et ne doivent pas être utilisés dans un environnement déployé.

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

L'authentification break-glass est désactivée par défaut dans `application.yml` ; la configuration locale l'active explicitement pour ces utilisateurs de démonstration.

GraphiQL et l'introspection GraphQL sont activés par la configuration local/dev pour le développement. La configuration de base garde GraphiQL et l'introspection désactivés.

## Vérifications Utiles

```sh
cd backend && ./gradlew test
cd backend && ./gradlew bootJar
cd frontend && npx -y pnpm@10.6.5 lint
cd frontend && npx -y pnpm@10.6.5 test
cd frontend && npx -y pnpm@10.6.5 build
```

La racine du dépôt fournit aussi :

```sh
make verify
```
