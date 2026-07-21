package com.bank.docgen.documentbrand;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * BDD-SYS-NORM-D1-011 — durable hard delete / irreversible retire of brand/entity persistence.
 */
class SysNormD1FlywayRetireMigrationTest {

    @Test
    void v76DropsCatalogTablesAndClearsBindings_bddD1011() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "db/migration/V76__retire_document_brand_legal_entity.sql"
        );
        assertThat(resource.exists()).isTrue();
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql).containsIgnoringCase("DROP TABLE");
        assertThat(sql).containsIgnoringCase("legal_entity");
        assertThat(sql).containsIgnoringCase("document_brand");
        assertThat(sql).containsIgnoringCase("default_legal_entity_code");
        assertThat(sql).containsIgnoringCase("allowed_document_brand_codes_json");
    }
}
