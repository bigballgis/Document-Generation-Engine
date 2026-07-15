package com.bank.docgen.template.api;

/**
 * Human-readable semantic change entry for CONTENT (and related) diffs.
 */
public record ChangeDiffHumanReadableEntry(
        String changeType,
        String path,
        String summary
) {
}
