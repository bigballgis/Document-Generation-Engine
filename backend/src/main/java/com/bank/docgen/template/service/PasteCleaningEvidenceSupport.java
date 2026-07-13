package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.authoring.structured.PasteCleaningSummary;
import com.bank.docgen.authoring.structured.PasteCleaningSummaryItem;
import com.bank.docgen.template.api.PasteCleaningEvidenceItemView;
import com.bank.docgen.template.api.PasteCleaningEvidenceView;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Serialize / deserialize non-sensitive paste-cleaning residue for anchor bindings.
 * Never persists source HTML or pasted plaintext.
 */
public final class PasteCleaningEvidenceSupport {

    private PasteCleaningEvidenceSupport() {
    }

    public static PasteCleaningEvidenceView fromSummary(PasteCleaningSummary summary) {
        if (summary == null) {
            return null;
        }
        List<PasteCleaningEvidenceItemView> items = new ArrayList<>();
        for (PasteCleaningSummaryItem item : summary.items()) {
            items.add(new PasteCleaningEvidenceItemView(
                    item.category(),
                    item.messageKey(),
                    sanitizeDetectionSummary(item.detectionSummary())
            ));
        }
        boolean unresolved = summary.blockedCount() > 0
                || items.stream().anyMatch(i -> i.category() == PasteCleaningCategory.BLOCKED);
        return new PasteCleaningEvidenceView(
                summary.transformedCount(),
                summary.removedCount(),
                summary.warningCount(),
                summary.blockedCount(),
                unresolved,
                items
        );
    }

    public static PasteCleaningEvidenceView sanitize(PasteCleaningEvidenceView evidence) {
        if (evidence == null) {
            return null;
        }
        List<PasteCleaningEvidenceItemView> items = new ArrayList<>();
        if (evidence.items() != null) {
            for (PasteCleaningEvidenceItemView item : evidence.items()) {
                if (item == null || item.category() == null || item.messageKey() == null || item.messageKey().isBlank()) {
                    continue;
                }
                items.add(new PasteCleaningEvidenceItemView(
                        item.category(),
                        item.messageKey().trim(),
                        sanitizeDetectionSummary(item.detectionSummary())
                ));
            }
        }
        int blockedFromItems = (int) items.stream()
                .filter(i -> i.category() == PasteCleaningCategory.BLOCKED)
                .count();
        int blockedCount = Math.max(evidence.blockedCount(), blockedFromItems);
        boolean unresolved = Boolean.TRUE.equals(evidence.unresolvedPasteBlockers())
                || blockedCount > 0
                || blockedFromItems > 0;
        return new PasteCleaningEvidenceView(
                Math.max(0, evidence.transformedCount()),
                Math.max(0, evidence.removedCount()),
                Math.max(0, evidence.warningCount()),
                blockedCount,
                unresolved,
                items
        );
    }

    public static boolean hasUnresolvedPasteBlockers(PasteCleaningEvidenceView evidence) {
        return evidence != null && evidence.hasUnresolvedPasteBlockers();
    }

    public static boolean hasUnresolvedPasteBlockers(String evidenceJson, ObjectMapper objectMapper) {
        return hasUnresolvedPasteBlockers(read(evidenceJson, objectMapper));
    }

    public static PasteCleaningEvidenceView read(String evidenceJson, ObjectMapper objectMapper) {
        if (evidenceJson == null || evidenceJson.isBlank()) {
            return null;
        }
        try {
            PasteCleaningEvidenceView parsed = objectMapper.readValue(evidenceJson, PasteCleaningEvidenceView.class);
            return sanitize(parsed);
        } catch (JsonProcessingException exception) {
            // Fail-closed: unreadable residue is treated as unresolved paste blockers.
            return new PasteCleaningEvidenceView(0, 0, 0, 1, true, List.of(
                    new PasteCleaningEvidenceItemView(
                            PasteCleaningCategory.BLOCKED,
                            "paste.summary.blocked",
                            "Unreadable paste-cleaning residue treated as unresolved blocker."
                    )
            ));
        }
    }

    public static String write(PasteCleaningEvidenceView evidence, ObjectMapper objectMapper) {
        if (evidence == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sanitize(evidence));
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidPasteCleaningEvidence");
        }
    }

    /**
     * Resolve residue for upsert: provided evidence replaces existing; null preserves;
     * clearPasteCleaningEvidence forces null.
     */
    public static String resolveForUpsert(
            String existingJson,
            PasteCleaningEvidenceView requested,
            Boolean clearPasteCleaningEvidence,
            ObjectMapper objectMapper
    ) {
        if (Boolean.TRUE.equals(clearPasteCleaningEvidence)) {
            return null;
        }
        if (requested != null) {
            PasteCleaningEvidenceView sanitized = sanitize(requested);
            // Clean Accept / rewrite with no unresolved blockers still persists residue (PB-C4)
            // and clears prior unresolved state by replacement.
            return write(sanitized, objectMapper);
        }
        return existingJson;
    }

    private static String sanitizeDetectionSummary(String detectionSummary) {
        if (detectionSummary == null || detectionSummary.isBlank()) {
            return null;
        }
        String trimmed = detectionSummary.trim();
        // Strip obvious HTML tags so residue never stores pasted markup snippets.
        return trimmed.replaceAll("(?is)<[^>]+>", "").trim();
    }
}
