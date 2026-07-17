package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.BindingValidationStatus;
import java.time.Instant;

public record AnchorBindingView(
        String id,
        String anchorId,
        String declaredContentType,
        String structuredContentJson,
        BindingValidationStatus validationStatus,
        PasteCleaningEvidenceView pasteCleaningEvidence,
        Instant updatedAt
) {
    public AnchorBindingView(
            String id,
            String anchorId,
            String declaredContentType,
            String structuredContentJson,
            BindingValidationStatus validationStatus
    ) {
        this(id, anchorId, declaredContentType, structuredContentJson, validationStatus, null, null);
    }

    public AnchorBindingView(
            String id,
            String anchorId,
            String declaredContentType,
            String structuredContentJson,
            BindingValidationStatus validationStatus,
            PasteCleaningEvidenceView pasteCleaningEvidence
    ) {
        this(id, anchorId, declaredContentType, structuredContentJson, validationStatus, pasteCleaningEvidence, null);
    }
}
