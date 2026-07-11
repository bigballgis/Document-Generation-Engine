package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoadSmokeConfigTest {

    @Test
    void defaultsMatchPlanAcceptanceFloors() {
        String previousSync = System.getProperty("docgen.loadSmoke.syncConcurrency");
        String previousSse = System.getProperty("docgen.loadSmoke.sseConcurrency");
        try {
            System.clearProperty("docgen.loadSmoke.syncConcurrency");
            System.clearProperty("docgen.loadSmoke.sseConcurrency");
            LoadSmokeConfig config = LoadSmokeConfig.fromEnvironment();
            assertThat(config.syncConcurrency()).isGreaterThanOrEqualTo(20);
            assertThat(config.sseConcurrency()).isGreaterThanOrEqualTo(5);
            assertThat(config.baseUrl()).isEqualTo(LoadSmokeConfig.DEFAULT_BASE_URL);
            assertThat(config.templateExternalId())
                    .isEqualTo(LoadSmokeConfig.DEFAULT_TEMPLATE_EXTERNAL_ID);
            assertThat(config.accessAccount())
                    .isEqualTo(LoadSmokeConfig.DEFAULT_ACCESS_ACCOUNT);
            assertThat(config.runtimeGenerateUrl("CORP-FOL-OFFER"))
                    .isEqualTo("http://localhost:8080/api/dev/v1/templates/CORP-FOL-OFFER/default/generate");
            assertThat(config.managementApiBase()).endsWith("/api/management/v1");
            assertThat(config.evidenceDir().toString()).contains("lrp-d6-load-smoke");
        } finally {
            restore("docgen.loadSmoke.syncConcurrency", previousSync);
            restore("docgen.loadSmoke.sseConcurrency", previousSse);
        }
    }

    @Test
    void mixedFormatsAlternateDocxAndPdf() {
        assertThat(LoadSmokeClients.mixedFormats(4)).containsExactly("DOCX", "PDF", "DOCX", "PDF");
        assertThat(LoadSmokeClients.mixedFormats(20)).hasSize(20);
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }
}
