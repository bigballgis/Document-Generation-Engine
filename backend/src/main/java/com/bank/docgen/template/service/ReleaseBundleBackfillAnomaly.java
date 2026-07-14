package com.bank.docgen.template.service;

import java.util.UUID;

/**
 * CE-K01 backfill anomaly — a PUBLISHED template version that could not be retroactively
 * pinned because its master / current revision / DOCX storage object is unavailable.
 * Recorded in the execution-sync ledger so operators can remediate.
 */
public record ReleaseBundleBackfillAnomaly(
        UUID templateId,
        String releaseVersion,
        String reason
) {
}
