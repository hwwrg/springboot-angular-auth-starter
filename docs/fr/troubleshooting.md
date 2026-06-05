[Index de la documentation](./README.md)

# Dépannage

## Le Backend ne Peut pas se Connecter à PostgreSQL

Vérifier que PostgreSQL tourne et que les variables datasource correspondent à la base :

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`

Pour le backend sans Docker, l'URL par défaut est `jdbc:postgresql://localhost:5432/authstarter`.

## Le Frontend ne Peut pas Joindre le Backend

Vérifier que le backend est sur `http://localhost:8080` et que la configuration frontend pointe vers :

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

Confirmer aussi `AUTH_STARTER_FRONTEND_ORIGIN=http://localhost:4200` pour le CORS local.

## Erreurs CSRF ou 403

Le frontend doit appeler `GET /auth/csrf` avant les requêtes GraphQL non sûres. Les requêtes non sûres doivent inclure `X-XSRF-TOKEN`, et les requêtes du navigateur doivent envoyer les credentials nécessaires.

Si les cookies ne sont pas envoyés localement, vérifier :

- `AUTH_STARTER_SESSION_COOKIE_SECURE=false`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE=lax`
- origins frontend et backend

## Échec du Login

En développement local, essayer :

- `operator@authstarter.local`
- `authstarter-local-password`

Ces identifiants sont local-only et nécessitent le profil `local`/`dev` ou Docker Compose. L'authentification break-glass est désactivée par défaut dans `application.yml` ; les utilisateurs break-glass configurés fonctionnent seulement quand `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

Si des tentatives publiques répétées échouent avec une erreur de rate limit, attendre l'expiration de la fenêtre courante. Les déploiements de production doivent utiliser un limiter distribué comme Redis au lieu du limiter en mémoire intégré.

## Page Admin Inaccessible

La frontend admin route et les backend admin operations demandent `SUPERADMIN` ou `ORG_ADMIN`. Le role `USER` est redirigé vers la not-authorized page.

## Email Non Envoyé

`AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock` enregistre des notification events mais n'envoie pas de vrai email. Utiliser `smtp` et configurer les variables `AUTH_STARTER_SMTP_*` pour un outil local de capture email ou un SMTP service réel.

## Conflits de Ports

Le setup local attend :

- frontend : `4200`
- backend : `8080`
- PostgreSQL : `5432`

Arrêter les services en conflit ou modifier la commande/configuration locale concernée.
