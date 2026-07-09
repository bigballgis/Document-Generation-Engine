package com.bank.docgen.template.port;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Read-only preview and batch-test evidence for template lifecycle gates.
 * Implemented by rendering; template must not read rendering persistence directly.
 */
public interface PreviewEvidencePort {

    int countSuccessfulPreviews(UUID templateId, UUID templateVersionId);

    int countFailedPreviews(UUID templateId, UUID templateVersionId);

    Set<String> successfulPreviewTestDataSetExternalIds(UUID templateId, UUID templateVersionId);

    Optional<BatchTestRunGateSnapshot> latestBatchTestRun(UUID templateId);
}
