[Index documentation](./README.md)

# Deployment

Ce repository contient un local development Compose setup et un backend Dockerfile. Il ne contient pas encore de production infrastructure ni de frontend container image séparée.

## Backend Image

[../../backend/Dockerfile](../../backend/Dockerfile) construit le Spring Boot jar avec Java 21, puis l'exécute sur une Java 21 JRE image. Le container expose le port `8080`.

L'entrypoint exécute :

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}
```

Définir `SPRING_PROFILES_ACTIVE` explicitement hors local development.

## Backend Runtime Configuration

Configurer au minimum :

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE`
- `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`

Pour SMTP delivery, configurer aussi :

- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`
- `AUTH_STARTER_SMTP_USERNAME`
- `AUTH_STARTER_SMTP_PASSWORD`
- `AUTH_STARTER_SMTP_AUTH`
- `AUTH_STARTER_SMTP_START_TLS`

Relire [../../.env.example](../../.env.example) et [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml) avant de déployer.

## Frontend Build

Construire l'Angular app :

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 build
```

La development environment pointe actuellement vers `http://localhost:8080`. `RuntimeConfigService` contient le support runtime config, et [../../frontend/public/config.template.json](../../frontend/public/config.template.json) montre l'expected shape, mais un environment file doit définir `runtimeConfigPath` pour que l'app charge une remote runtime config.

## Local Compose Scope

[../../docker-compose.yml](../../docker-compose.yml) démarre PostgreSQL et le backend pour local development. Ce n'est pas un production deployment template.

Avant d'utiliser ce starter dans un système déployé, vérifier cookie security, HTTPS, CORS origins, SMTP, password policy, monitoring, backup, retention et operational access.
