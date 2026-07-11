package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LoadSmokeClientsSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void stripUtf8BomRemovesLeadingBom() {
        assertThat(LoadSmokeClients.stripUtf8Bom("\uFEFF{\"variables\":{}}"))
                .isEqualTo("{\"variables\":{}}");
        assertThat(LoadSmokeClients.stripUtf8Bom("{\"variables\":{}}"))
                .isEqualTo("{\"variables\":{}}");
        assertThat(LoadSmokeClients.stripUtf8Bom(null)).isNull();
    }

    @Test
    void loadVariablesToleratesUtf8BomFile() throws Exception {
        Path vars = tempDir.resolve("vars.json");
        Files.writeString(vars, "\uFEFF{\"variables\":{\"partyLegalName\":\"Acme\"}}", StandardCharsets.UTF_8);

        String previous = System.getProperty("docgen.loadSmoke.variablesFile");
        try {
            System.setProperty("docgen.loadSmoke.variablesFile", vars.toString());
            LoadSmokeConfig config = LoadSmokeConfig.fromEnvironment();
            JsonNode variables = new LoadSmokeClients(config, new ObjectMapper()).loadVariables();
            assertThat(variables.path("partyLegalName").asText()).isEqualTo("Acme");
        } finally {
            if (previous == null) {
                System.clearProperty("docgen.loadSmoke.variablesFile");
            } else {
                System.setProperty("docgen.loadSmoke.variablesFile", previous);
            }
        }
    }

    @Test
    void accessAccountIsConfigurable() {
        String previous = System.getProperty("docgen.loadSmoke.accessAccount");
        try {
            System.clearProperty("docgen.loadSmoke.accessAccount");
            assertThat(LoadSmokeConfig.fromEnvironment().accessAccount())
                    .isEqualTo(LoadSmokeConfig.DEFAULT_ACCESS_ACCOUNT);
            System.setProperty("docgen.loadSmoke.accessAccount", "svc-caller");
            assertThat(LoadSmokeConfig.fromEnvironment().accessAccount()).isEqualTo("svc-caller");
        } finally {
            if (previous == null) {
                System.clearProperty("docgen.loadSmoke.accessAccount");
            } else {
                System.setProperty("docgen.loadSmoke.accessAccount", previous);
            }
        }
    }
}
