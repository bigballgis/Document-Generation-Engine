package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.RenderingOperationException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class ArtifactSpoolServiceTest {

    @Test
    void spoolWritesTempFileAndEnforcesSizeLimit() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setMaxGeneratedArtifactBytes(8);
        GeneratedArtifactSizeGuard guard = new GeneratedArtifactSizeGuard(properties);
        ArtifactSpoolService spoolService = new ArtifactSpoolService(guard);
        byte[] payload = new byte[]{10, 20, 30};

        try (SpooledArtifact spooled = spoolService.spool(payload)) {
            assertThat(spooled.sizeBytes()).isEqualTo(3);
            assertThat(Files.exists(spooled.path())).isTrue();
            assertThat(spooled.openInputStream().readAllBytes()).containsExactly(10, 20, 30);
        }

        assertThatThrownBy(() -> spoolService.spool(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9}))
                .isInstanceOf(RenderingOperationException.class)
                .hasMessage("api.error.generation.artifactTooLarge");
    }
}
