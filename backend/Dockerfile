FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/build/libs/*.jar /app/springboot-angular-auth-starter-backend.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar /app/springboot-angular-auth-starter-backend.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-local}"]
