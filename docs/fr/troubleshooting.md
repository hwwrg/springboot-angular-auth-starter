[Index documentation](./README.md)

# Troubleshooting

## Backend Cannot Connect to PostgreSQL

Vérifier que PostgreSQL tourne et que les datasource variables correspondent à la base :

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`

Pour le backend sans Docker, l'URL par défaut est `jdbc:postgresql://localhost:5432/authstarter`.

## Frontend Cannot Reach Backend

Vérifier que le backend est sur `http://localhost:8080` et que la frontend config pointe vers :

- `backendBaseUrl`: `http://localhost:8080`
- `graphql.endpoint`: `http://localhost:8080/graphql`

Confirmer aussi `AUTH_STARTER_FRONTEND_ORIGIN=http://localhost:4200` pour le CORS local.

## CSRF or 403 Errors

Le frontend doit appeler `GET /auth/csrf` avant les unsafe GraphQL requests. Les unsafe requests doivent inclure `X-XSRF-TOKEN`, et les browser requests doivent include credentials.

Si les cookies ne sont pas envoyés localement, vérifier :

- `AUTH_STARTER_SESSION_COOKIE_SECURE=false`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE=lax`
- frontend and backend origins

## Login Fails

En local development, essayer :

- `operator@authstarter.local`
- `authstarter-local-password`

Si les DB-backed credentials ont été changés, le configured break-glass user fonctionne seulement quand `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED=true`.

## Admin Page Is Not Accessible

La frontend admin route et les backend admin operations demandent `SUPERADMIN` ou `ORG_ADMIN`. Le role `USER` est redirigé vers la not-authorized page.

## Email Is Not Delivered

`AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER=local-mock` enregistre des notification events mais n'envoie pas de vrai email. Utiliser `smtp` et configurer les variables `AUTH_STARTER_SMTP_*` pour un local mail catcher ou un SMTP service réel.

## Port Conflicts

Le local setup attend :

- frontend : `4200`
- backend : `8080`
- PostgreSQL : `5432`

Arrêter les services en conflit ou modifier la local command/configuration concernée.
