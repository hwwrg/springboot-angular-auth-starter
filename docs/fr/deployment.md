[Index de la documentation](./README.md)

# Déploiement

Ce dépôt contient une configuration Compose pour le développement local et un Dockerfile backend. Il ne contient pas encore d'infrastructure de production ni d'image de conteneur frontend séparée.

## Image Backend

[../../backend/Dockerfile](../../backend/Dockerfile) construit le Spring Boot jar avec Java 21, puis l'exécute sur une Java 21 JRE image. Le conteneur expose le port `8080`.

L'entrypoint exécute :

```sh
java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}
```

Définir `SPRING_PROFILES_ACTIVE` explicitement hors développement local.

## Configuration Runtime du Backend

Configurer au minimum :

- `AUTH_STARTER_DATASOURCE_URL`
- `AUTH_STARTER_DATASOURCE_USERNAME`
- `AUTH_STARTER_DATASOURCE_PASSWORD`
- `AUTH_STARTER_FRONTEND_ORIGIN`
- `AUTH_STARTER_SESSION_COOKIE_SECURE`
- `AUTH_STARTER_SESSION_COOKIE_SAME_SITE`
- `AUTH_STARTER_BASELINE_AUTH_BREAK_GLASS_ENABLED`
- `AUTH_STARTER_NOTIFICATION_EMAIL_PROVIDER`

Pour la livraison SMTP, configurer aussi :

- `AUTH_STARTER_SMTP_HOST`
- `AUTH_STARTER_SMTP_PORT`
- `AUTH_STARTER_SMTP_USERNAME`
- `AUTH_STARTER_SMTP_PASSWORD`
- `AUTH_STARTER_SMTP_AUTH`
- `AUTH_STARTER_SMTP_START_TLS`

Relire [../../.env.example](../../.env.example) et [../../backend/src/main/resources/application.yml](../../backend/src/main/resources/application.yml) avant de déployer.

## Build Frontend

Construire l'Angular app :

```sh
cd frontend
npx -y pnpm@10.6.5 install --frozen-lockfile
npx -y pnpm@10.6.5 build
```

L'environnement de développement pointe actuellement vers `http://localhost:8080`. `RuntimeConfigService` contient le support runtime config, et [../../frontend/public/config.template.json](../../frontend/public/config.template.json) montre le format attendu, mais un fichier d'environnement doit définir `runtimeConfigPath` pour que l'application charge une runtime config distante.

## Portée du Compose Local

[../../docker-compose.yml](../../docker-compose.yml) démarre PostgreSQL et le backend pour le développement local. Ce n'est pas un modèle de déploiement en production.

Avant d'utiliser ce starter dans un système déployé, vérifier cookie security, HTTPS, CORS origins, SMTP, password policy, monitoring, backup, retention et operational access.
