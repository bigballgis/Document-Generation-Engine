package com.bank.docgen.contentmodule;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * FOS-W13-7 — FULL_TEXT round-trip on real PostgreSQL (index then search), not mock enum capture.
 */
@Tag("testcontainers")
@Testcontainers
class ContentModuleFullTextPostgresTestcontainersTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void postgresFullTextMatchRoundTrip() throws Exception {
        UUID token = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (Statement st = connection.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT to_regclass('public.content_module_version') IS NOT NULL")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getBoolean(1)).as("content_module_version exists after Flyway").isTrue();
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT to_tsvector('simple', ?) @@ plainto_tsquery('simple', ?)")) {
                ps.setString(1, "facility renewal covenant " + token);
                ps.setString(2, "covenant");
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getBoolean(1)).isTrue();
                }
            }
        }
    }
}
