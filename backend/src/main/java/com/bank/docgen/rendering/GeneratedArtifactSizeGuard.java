package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
import org.springframework.stereotype.Component;

/** Enforces configured maximum artifact size before persistence (SOR-P02). */
@Component
public class GeneratedArtifactSizeGuard {

    private final DocgenRenderingProperties renderingProperties;

    public GeneratedArtifactSizeGuard(DocgenRenderingProperties renderingProperties) {
        this.renderingProperties = renderingProperties;
    }

    public void assertWithinLimit(byte[] artifactBytes) {
        if (artifactBytes.length > renderingProperties.getMaxGeneratedArtifactBytes()) {
            throw new TemplateValidationException("api.error.generation.artifactTooLarge");
        }
    }
}
