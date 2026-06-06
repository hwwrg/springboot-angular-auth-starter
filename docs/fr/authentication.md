[Index de la documentation](./README.md)

# Authentification

## Modèle

L'authentification est basée sur les sessions. Le backend crée une session côté serveur et envoie le cookie `AUTH_STARTER_SESSION`. Le frontend Angular envoie les requêtes backend avec les credentials nécessaires.

La protection CSRF est activée. Le frontend initialise le token via :

- `GET /auth/csrf`

Les requêtes non sûres utilisent :

- `X-XSRF-TOKEN`

## Opérations Publiques

Le backend autorise ces GraphQL root fields sans session authentifiée :

- Query : `readiness`, `currentSession`
- Mutation : `login`, `logout`, `acceptUserInvite`, `requestPasswordReset`, `resetPassword`

Les autres opérations GraphQL demandent une session authentifiée.

## Sources de Login

`BaselineAuthService` essaie d'abord les identifiants stockés en base lorsque `UserCredentialAuthenticationService` est disponible. Si cela échoue ou si aucun identifiant persistant ne correspond, les utilisateurs break-glass configurés peuvent s'authentifier uniquement quand `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

L'authentification break-glass est désactivée par défaut dans [application.yml](../../backend/src/main/resources/application.yml), et ce fichier ne fournit pas de username ou password baseline utilisable en production. Les identifiants de démonstration local-only sont définis uniquement dans la configuration locale :

- [application-local.yml](../../backend/src/main/resources/application-local.yml)
- [application-dev.yml](../../backend/src/main/resources/application-dev.yml)
- [docker-compose.yml](../../docker-compose.yml)

Ne pas utiliser les identifiants de démonstration locaux dans un environnement déployé.

## Rate Limiting

Les mutations publiques d'authentification utilisent un rate limiter basique en mémoire :

- `login`
- `requestPasswordReset`
- `resetPassword`
- `acceptUserInvite`

C'est une protection adaptée à un seul processus backend de starter. En production ou avec plusieurs instances, la remplacer ou l'appuyer sur un stockage distribué comme Redis.

## Protection Contre les Abus GraphQL

Les requêtes GraphQL ont des limites configurables de profondeur, complexité et taille de corps :

- `AUTH_STARTER_GRAPHQL_MAX_QUERY_DEPTH`
- `AUTH_STARTER_GRAPHQL_MAX_QUERY_COMPLEXITY`
- `AUTH_STARTER_GRAPHQL_MAX_REQUEST_BYTES`

GraphiQL est désactivé par défaut. L'introspection GraphQL est contrôlée par `AUTH_STARTER_GRAPHQL_INTROSPECTION_ENABLED` ; la configuration de base la désactive, tandis que la configuration local/dev l'active pour le développement.

## RBAC

Les roles sont représentés par `AuthStarterRole` :

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

L'Angular admin route utilise `roleAccessGuard`. Le backend admin management vérifie le principal authentifié et le current organization context. Seul `SUPERADMIN` peut attribuer ou modifier `SUPERADMIN` ; `ORG_ADMIN` ne peut pas modifier les utilisateurs `SUPERADMIN`, ne peut pas changer son propre role/status, et ne peut pas attribuer `ORG_ADMIN` sauf si la policy backend l'autorise explicitement.

## Invitation and Password Reset

Les invitation et password reset tokens sont stockés sous forme de hash dans `user_security_tokens`. Admin user creation appelle l'invitation service et le notification service. Password reset request renvoie un message de succès générique pour ne pas révéler l'existence d'un compte.

La livraison email est sélectionnée avec :

- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=smtp`

## Points d'Entrée Importants

- `POST /graphql`
- `GET /auth/csrf`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
