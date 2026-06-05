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

`BaselineAuthService` essaie d'abord les identifiants stockés en base lorsque `UserCredentialAuthenticationService` est disponible. Si cela échoue ou si aucun identifiant persistant ne correspond, les utilisateurs break-glass configurés peuvent s'authentifier quand `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

Les utilisateurs locaux configurés viennent de :

- [application.yml](../../backend/src/main/resources/application.yml)
- [application-local.yml](../../backend/src/main/resources/application-local.yml)

## RBAC

Les roles sont représentés par `AuthStarterRole` :

- `SUPERADMIN`
- `ORG_ADMIN`
- `USER`

L'Angular admin route utilise `roleAccessGuard`. Le backend admin management vérifie le principal authentifié et le current organization context.

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
