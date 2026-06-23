FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S docgen && adduser -S docgen -G docgen
COPY target/docgen-backend-*.jar app.jar
USER docgen
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
