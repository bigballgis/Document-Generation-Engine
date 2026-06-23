package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArtifactContentTypesTest {

    @Test
    void derivesPdfContentTypeFromStorageKey() {
        assertThat(ArtifactContentTypes.fromStorageKey("generated/DOC-1/output.pdf"))
                .isEqualTo("application/pdf");
    }

    @Test
    void derivesDocxContentTypeFromStorageKey() {
        assertThat(ArtifactContentTypes.fromStorageKey("generated/DOC-1/output.docx"))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
