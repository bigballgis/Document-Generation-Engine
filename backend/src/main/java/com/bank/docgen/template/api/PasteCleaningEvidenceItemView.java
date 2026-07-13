package com.bank.docgen.template.api;

import com.bank.docgen.authoring.structured.PasteCleaningCategory;

/**
 * Non-sensitive paste-cleaning evidence item (counts / messageKey only — no source HTML).
 */
public record PasteCleaningEvidenceItemView(
        PasteCleaningCategory category,
        String messageKey,
        String detectionSummary
) {
}
