package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
import org.junit.jupiter.api.Test;

class GeneratedArtifactSizeGuardTest {

    @Test
    void rejectsArtifactsAboveConfiguredLimit() {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setMaxGeneratedArtifactBytes(4);
        GeneratedArtifactSizeGuard guard = new GeneratedArtifactSizeGuard(properties);

        assertThatThrownBy(() -> guard.assertWithinLimit(new byte[]{1, 2, 3, 4, 5}))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.artifactTooLarge");
    }
}
