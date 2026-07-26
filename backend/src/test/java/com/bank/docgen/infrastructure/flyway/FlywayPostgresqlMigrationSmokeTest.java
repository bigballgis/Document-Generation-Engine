package com.bank.docgen.infrastructure.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * FOS-W13-1 / IBL-D1 — Testcontainers PostgreSQL + Flyway migrate then schema presence assert.
 *
 * <p>Excluded from default {@code mvn verify} via Surefire {@code excludedGroups=testcontainers}.
 * Run with {@code -Ptestcontainers} (also wired in Constitution Gates CI job).
 */
@Tag("testcontainers")
@Testcontainers
class FlywayPostgresqlMigrationSmokeTest {

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

    @Test
    void flywayMigrateThenCoreTablesExistForValidateHonesty() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();
        assertThat(flyway.migrate().success).isTrue();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement statement = connection.createStatement()) {
            for (String table : new String[] {
                "flyway_schema_history",
                "templates",
                "template_versions",
                "api_policies",
                "api_credentials"
            }) {
                try (ResultSet rs = statement.executeQuery(
                        "SELECT to_regclass('public." + table + "') IS NOT NULL")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBoolean(1))
                            .as("expected table public.%s after Flyway migrate (ddl-auto validate honesty)", table)
                            .isTrue();
                }
            }
        }
    }
}
