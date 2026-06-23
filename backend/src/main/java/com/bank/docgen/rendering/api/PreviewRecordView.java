package com.bank.docgen.rendering.api;

import com.bank.docgen.rendering.domain.PreviewStatus;
import java.time.Instant;
import java.util.List;

public record PreviewRecordView(
        String previewId,
        String templateId,
        String templateVersionId,
        PreviewStatus status,
        String outputFormat,
        String artifactStorageKey,
        List<FidelityWarningView> fidelityWarnings,
        String comparisonSummary,
        String testDataSetId,
        Instant createdAt
) {
}
