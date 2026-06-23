# Optional all-in-one build (slow inside BuildKit). Prefer Dockerfile.packaged + deploy script.
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache curl \
    && addgroup -S docgen && adduser -S docgen -G docgen
COPY --from=build /app/target/docgen-backend-*.jar app.jar
USER docgen
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --retries=10 \
  CMD curl -f http://localhost:8080/healthz || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
