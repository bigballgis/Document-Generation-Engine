FROM maven:3.9.9-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B package -DskipTests

# Runtime uses Debian jammy (not Alpine) because Alpine 3.23 lacks fonts-crosextra-caladea;
# Carlito + Caladea metric-compatible substitutes are required by LR-A2 / ADR-0041 (CD-PIT-01).
# Task #51 / ADR-0028: Temurin 25 (was 21).
FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libreoffice-core \
        libreoffice-writer \
        fonts-dejavu \
        fontconfig \
        fonts-noto-cjk \
        fonts-crosextra-carlito \
        fonts-crosextra-caladea \
    && fc-cache -f \
    && sh -c 'fc-list :lang=zh | grep -q . || { echo "LR-A2: no CJK fonts (fc-list :lang=zh empty)" >&2; exit 1; }' \
    && sh -c 'fc-list | grep -qi carlito || { echo "LR-A2: Carlito font missing" >&2; exit 1; }' \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 65532 nonroot \
    && useradd --system --uid 65532 --gid nonroot --home-dir /tmp --shell /usr/sbin/nologin nonroot \
    && chown -R nonroot:nonroot /app
COPY --from=build --chown=nonroot:nonroot /app/target/docgen-backend-*.jar app.jar
USER 65532:65532
EXPOSE 8080
ENV HOME=/tmp
ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/tmp"
ENTRYPOINT ["java", "-jar", "app.jar"]
