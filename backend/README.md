# Backend

Java 21 + Spring Boot 3 module-first layout under `com.bank.docgen`.

## Packages

| Package | Responsibility |
| --- | --- |
| `sharedkernel.api` | Error envelope, metadata, global exception handling |
| `sharedkernel.health` | `/healthz`, `/readyz` |
| `sharedkernel.security` | Argon2 password hashing, JWT skeleton |
| `infrastructure.*` | Spring configuration, i18n |

## Run

```powershell
mvn -B -ntp verify
mvn spring-boot:run
```

Requires PostgreSQL for `spring-boot:run` (see root `docker-compose.yml`).
Tests use in-memory H2 (`application-test.yml`).
