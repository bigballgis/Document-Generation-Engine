package com.bank.docgen.rendering.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.rendering.domain.PreviewStatus;
import java.time.Instant;
import java.util.List;

public record PreviewRecordView(
        String previewId,
        String templateId,
        String templateVersionId,
        PreviewStatus status,
        String outputFormat,
        String renderProfileVersion,
        String artifactStorageKey,
        String pdfArtifactStorageKey,
        List<FidelityWarningView> fidelityWarnings,
        PreviewComparisonView previewComparison,
        String testDataSetId,
        Instant createdAt
) {
    public PreviewRecordView {
        fidelityWarnings = DefensiveCopies.copyList(fidelityWarnings);
    }

}
