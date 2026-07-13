package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.AnchorContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertAnchorBindingRequest(
        @NotBlank String anchorId,
        @NotNull AnchorContentType declaredContentType,
        @NotBlank String structuredContentJson,
        @Valid PasteCleaningEvidenceView pasteCleaningEvidence,
        Boolean clearPasteCleaningEvidence
) {
    public UpsertAnchorBindingRequest(
            String anchorId,
            AnchorContentType declaredContentType,
            String structuredContentJson
    ) {
        this(anchorId, declaredContentType, structuredContentJson, null, null);
    }
}
