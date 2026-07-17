package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.AnchorContentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record UpsertAnchorBindingRequest(
        @NotBlank String anchorId,
        @NotNull AnchorContentType declaredContentType,
        @NotBlank String structuredContentJson,
        @Valid PasteCleaningEvidenceView pasteCleaningEvidence,
        Boolean clearPasteCleaningEvidence,
        Instant expectedUpdatedAt
) {
    public UpsertAnchorBindingRequest(
            String anchorId,
            AnchorContentType declaredContentType,
            String structuredContentJson
    ) {
        this(anchorId, declaredContentType, structuredContentJson, null, null, null);
    }

    public UpsertAnchorBindingRequest(
            String anchorId,
            AnchorContentType declaredContentType,
            String structuredContentJson,
            PasteCleaningEvidenceView pasteCleaningEvidence,
            Boolean clearPasteCleaningEvidence
    ) {
        this(
                anchorId,
                declaredContentType,
                structuredContentJson,
                pasteCleaningEvidence,
                clearPasteCleaningEvidence,
                null
        );
    }
}
