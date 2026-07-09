package com.bank.docgen.template.port;

import java.util.UUID;

/**
 * Minimal template identity for rendering orchestration without reaching into template services.
 */
public record RenderableTemplateSnapshot(
        UUID templateId,
        UUID masterId,
        String groupCode
) {
}
