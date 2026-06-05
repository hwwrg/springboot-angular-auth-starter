FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN groupadd --system authstarter \
    && useradd --system --gid authstarter --home-dir /app --shell /usr/sbin/nologin authstarter
COPY --from=build /workspace/build/libs/*.jar /app/springboot-angular-auth-starter-backend.jar
RUN chown -R authstarter:authstarter /app
USER authstarter

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}"]
