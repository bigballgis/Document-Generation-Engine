package com.bank.docgen.template.port;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.UUID;

/**
 * Minimal template identity for rendering orchestration without reaching into template services.
 */
public record RenderableTemplateSnapshot(
        UUID templateId,
        UUID masterId,
        String groupCode,
        List<String> allowedDocumentBrandCodes
) {
    public RenderableTemplateSnapshot {
        allowedDocumentBrandCodes = DefensiveCopies.copyList(allowedDocumentBrandCodes);
    }

    /** Compatibility constructor for callers that omit document-brand allow-list. */
    public RenderableTemplateSnapshot(UUID templateId, UUID masterId, String groupCode) {
        this(templateId, masterId, groupCode, List.of());
    }
}
