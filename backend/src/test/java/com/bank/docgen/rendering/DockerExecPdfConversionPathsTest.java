package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DockerExecPdfConversionPathsTest {

    @Test
    void containerPathsAreUniquePerInvocation() {
        String a = DockerExecPdfConversionService.containerInputPath("docgen-docker-pdf-111");
        String b = DockerExecPdfConversionService.containerInputPath("docgen-docker-pdf-222");
        assertThat(a).isNotEqualTo(b);
        assertThat(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-111"))
                .isNotEqualTo(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-222"));
        assertThat(a).contains("docgen-docker-pdf-111");
        assertThat(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-111"))
                .endsWith("/docgen-input-docgen-docker-pdf-111.pdf");
        assertThat(DockerExecPdfConversionService.containerOutputPath("docgen-docker-pdf-111"))
                .startsWith(DockerExecPdfConversionService.containerOutDir("docgen-docker-pdf-111"));
    }
}
