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

- `postgres` sur le host port `5432`
- `backend` sur le host port `8080`

Le fichier Compose courant définit directement les variables locales du backend. [../../.env.example](../../.env.example) sert de référence pour les valeurs configurables.

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

- `operator@authstarter.local` / `authstarter-local-password` / `SUPERADMIN`
- `org-admin@authstarter.local` / `authstarter-local-password` / `ORG_ADMIN`
- `user@authstarter.local` / `authstarter-local-password` / `USER`

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
