package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GeneratedArtifactSizeGuardTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Test
    void rejectsArtifactsAboveConfiguredLimit() {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setMaxGeneratedArtifactBytes(4);
        GeneratedArtifactSizeGuard guard = new GeneratedArtifactSizeGuard(properties);

        assertThatThrownBy(() -> guard.assertWithinLimit(new byte[]{1, 2, 3, 4, 5}))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.artifactTooLarge");
    }

    @Test
    void rejectsSpooledFilesAboveConfiguredLimit() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setMaxGeneratedArtifactBytes(4);
        GeneratedArtifactSizeGuard guard = new GeneratedArtifactSizeGuard(properties);
        java.nio.file.Path artifactFile = tempDir.resolve("artifact.bin");
        Files.write(artifactFile, new byte[]{1, 2, 3, 4, 5});

        assertThatThrownBy(() -> guard.assertWithinLimit(artifactFile))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessage("api.error.generation.artifactTooLarge");
    }

    @Test
    void acceptsSpooledFilesWithinConfiguredLimit() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setMaxGeneratedArtifactBytes(8);
        GeneratedArtifactSizeGuard guard = new GeneratedArtifactSizeGuard(properties);
        java.nio.file.Path artifactFile = tempDir.resolve("artifact.bin");
        Files.write(artifactFile, new byte[]{1, 2, 3});

        guard.assertWithinLimit(artifactFile);

        assertThat(Files.size(artifactFile)).isEqualTo(3);
    }
}
