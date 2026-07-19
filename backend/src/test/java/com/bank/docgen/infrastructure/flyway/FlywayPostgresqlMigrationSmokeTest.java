package com.bank.docgen.infrastructure.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * IBL-D1 / F20 — Testcontainers PostgreSQL + Flyway-on smoke.
 *
 * <p>Excluded from default {@code mvn verify} via Surefire {@code excludedGroups=testcontainers}.
 * Run with {@code -Ptestcontainers}. Broken or PG-incompatible migrations fail this lane
 * (not silent green).
 */
@Tag("testcontainers")
@Testcontainers
class FlywayPostgresqlMigrationSmokeTest {

    /** Align with compose acceptance stack ({@code docker-compose.yml}). */
    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    @Test
    void flywayMigrationsApplyOnPostgresql() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).as("Flyway migrate must succeed on real PostgreSQL").isTrue();
        assertThat(result.migrationsExecuted).as("at least one migration must execute").isPositive();
        assertThat(flyway.info().applied()).as("flyway_schema_history must record applied versions").isNotEmpty();
        assertThat(flyway.info().current()).isNotNull();
        assertThat(flyway.info().current().getVersion()).isNotNull();
    }
}
