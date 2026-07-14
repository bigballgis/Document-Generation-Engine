package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-G02-RT-003 — DocumentGenerationAssemblySupport must not call specimen watermark helpers.
 */
class DocumentGenerationAssemblySupportNoWatermarkTest {

    @Test
    void assemblySupportSourceDoesNotReferenceSpecimenStampers() throws Exception {
        Path source = Path.of("src/main/java/com/bank/docgen/runtime/service/DocumentGenerationAssemblySupport.java");
        String text = Files.readString(source, StandardCharsets.UTF_8);
        assertThat(text)
                .doesNotContain("DocxSpecimenWatermarkStamper")
                .doesNotContain("PdfSpecimenWatermarkStamper")
                .doesNotContain("SPECIMEN");
    }
}
