package com.bank.docgen.template.service;

import java.util.List;

/**
 * CE-K01 backfill outcome. {@code pinnedCount} is the number of rows whose pin fields were
 * written this run; {@code anomalyCount} is the number of rows that could not be pinned
 * (BDD-CE-K01-019) and remain fail-closed at runtime.
 */
public record ReleaseBundleBackfillResult(
        int pinnedCount,
        int anomalyCount,
        List<ReleaseBundleBackfillAnomaly> anomalies
) {
    public ReleaseBundleBackfillResult {
        anomalies = List.copyOf(anomalies);
    }
}