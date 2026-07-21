package com.bank.docgen.authorization.management.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BDD-SYS-NORM-ROLE-001 / 002 / 006 / 007 / 008 — Flyway remap script is present and idempotent.
 */
class RoleCompressionMigrationScriptTest {

    @Test
    void v75RemapsRetiredRolesIdempotently() throws IOException {
        Path migration = Path.of("src/main/resources/db/migration/V75__sys_norm_role_compression.sql");
        assertThat(migration).exists();
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertThat(sql).containsIgnoringCase("GROUP_ADMIN");
        assertThat(sql).containsIgnoringCase("GROUP_ADMIN");
        assertThat(sql).containsIgnoringCase("DOCUMENT_AUTHOR");
        assertThat(sql).containsIgnoringCase("DOCUMENT_AUTHOR");
        assertThat(sql).containsIgnoringCase("DOCUMENT_AUTHOR");
        assertThat(sql).containsIgnoringCase("ON CONFLICT");
        assertThat(sql).containsIgnoringCase("DELETE FROM management_user_role");
    }
}
