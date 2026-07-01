package com.bank.docgen.rendering.api;

import com.bank.docgen.rendering.domain.PreviewStatus;
import java.time.Instant;

public record PreviewSummaryView(
        String previewId,
        String templateVersionId,
        PreviewStatus status,
        String testDataSetId,
        Instant createdAt,
        String createdBy,
        int fidelityWarningCount,
        int comparisonBlockerCount,
        int comparisonWarningCount,
        boolean docxAvailable,
        boolean pdfAvailable
) {
}
