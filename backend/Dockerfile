FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN apk add --no-cache \
        libreoffice \
        ttf-dejavu \
        fontconfig \
    && addgroup -g 65532 -S nonroot \
    && adduser -S -u 65532 -G nonroot nonroot \
    && chown -R nonroot:nonroot /app
COPY --from=build --chown=nonroot:nonroot /app/target/docgen-backend-*.jar app.jar
USER 65532:65532
EXPOSE 8080
ENV HOME=/tmp
ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/tmp"
ENTRYPOINT ["java", "-jar", "app.jar"]
