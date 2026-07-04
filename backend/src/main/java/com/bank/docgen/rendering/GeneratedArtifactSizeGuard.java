package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/** Enforces configured maximum artifact size before persistence (SOR-P02). */
@Component
public class GeneratedArtifactSizeGuard {

    private final DocgenRenderingProperties renderingProperties;

    public GeneratedArtifactSizeGuard(DocgenRenderingProperties renderingProperties) {
        this.renderingProperties = renderingProperties;
    }

    public void assertWithinLimit(byte[] artifactBytes) {
        assertWithinLimit(artifactBytes.length);
    }

    public void assertWithinLimit(Path artifactFile) throws IOException {
        assertWithinLimit(Files.size(artifactFile));
    }

    private void assertWithinLimit(long artifactByteCount) {
        if (artifactByteCount > renderingProperties.getMaxGeneratedArtifactBytes()) {
            throw new TemplateValidationException("api.error.generation.artifactTooLarge");
        }
    }
}
