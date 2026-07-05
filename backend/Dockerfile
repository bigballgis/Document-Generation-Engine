FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
# LR-A2 / ADR-0041: rendering font baseline — CJK + metric-compatible substitutes
# (Carlito/Caladea) for Calibri/Cambria so bank correspondence renders without tofu or
# layout drift (CD-PIT-01).
RUN apk add --no-cache \
        libreoffice \
        ttf-dejavu \
        fontconfig \
        font-noto-cjk \
        font-crosextra-carlito \
        font-crosextra-caladea \
    && fc-cache -f \
    && addgroup -g 65532 -S nonroot \
    && adduser -S -u 65532 -G nonroot nonroot \
    && chown -R nonroot:nonroot /app
COPY --from=build --chown=nonroot:nonroot /app/target/docgen-backend-*.jar app.jar
USER 65532:65532
EXPOSE 8080
ENV HOME=/tmp
ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/tmp"
ENTRYPOINT ["java", "-jar", "app.jar"]
