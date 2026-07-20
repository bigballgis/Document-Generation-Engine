package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.port.TemplateCoveragePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package-private execution helpers for async batch test runs.
 */
final class AsyncBatchTestExecutionSupport {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncBatchTestExecutionSupport.class);

    private static final String DIMENSION_ANCHOR_BINDINGS = "ANCHOR_BINDINGS";
    private static final String DIMENSION_REQUIRED_VARIABLES = "REQUIRED_VARIABLES";
    private static final String DIMENSION_REQUIRED_SAMPLES = "REQUIRED_SAMPLES";

    private final PreviewGenerationService previewGenerationService;
    private final BatchTestRunRepository batchTestRunRepository;
    private final TemplateCoveragePort templateCoveragePort;
    private final SseEmitterRegistry batchSseRegistry;
    private final ObjectMapper objectMapper;
    private final int maxVisibleRuns;

    AsyncBatchTestExecutionSupport(
            PreviewGenerationService previewGenerationService,
            BatchTestRunRepository batchTestRunRepository,
            TemplateCoveragePort templateCoveragePort,
            SseEmitterRegistry batchSseRegistry,
            ObjectMapper objectMapper,
            int maxVisibleRuns
    ) {
        this.previewGenerationService = previewGenerationService;
        this.batchTestRunRepository = batchTestRunRepository;
        this.templateCoveragePort = templateCoveragePort;
        this.batchSseRegistry = batchSseRegistry;
        this.objectMapper = objectMapper;
        this.maxVisibleRuns = maxVisibleRuns;
    }

    void executeBatchRun(
            UUID templateId,
            BatchTestRunEntity run,
            List<String> dataSetIds,
            ManagementSessionClaims session
    ) {
        UUID runId = run.getId();
        try {
            int total = dataSetIds.size();
            List<AsyncBatchTestOrchestrator.SampleResult> results = new ArrayList<>();
            int succeededCount = 0;
            int failedCount = 0;

            for (int i = 0; i < dataSetIds.size(); i++) {
                String dataSetId = dataSetIds.get(i);
                batchSseRegistry.send(runId, "sample_started", Map.of(
                        "sampleIndex", i + 1,
                        "totalSamples", total,
                        "dataSetExternalId", dataSetId
                ));

                boolean success;
                String errorDetail = null;
                PreviewRecordView preview = null;
                try {
                    preview = previewGenerationService.runTestGenerateForBatch(
                            templateId, dataSetId, runId, session
                    );
                    success = preview.status() == PreviewStatus.SUCCEEDED;
                } catch (Exception ex) {
                    LOG.warn("Batch sample {} failed: {}", dataSetId, ex.getMessage());
                    success = false;
                    errorDetail = ex.getMessage();
                }

                if (success) {
                    succeededCount++;
                    results.add(new AsyncBatchTestOrchestrator.SampleResult(
                            dataSetId,
                            true,
                            null,
                            preview.previewId(),
                            preview.artifactStorageKey(),
                            preview.pdfArtifactStorageKey()
                    ));
                    batchSseRegistry.send(runId, "sample_done", Map.of(
                            "sampleIndex", i + 1,
                            "success", true,
                            "dataSetExternalId", dataSetId
                    ));
                } else {
                    failedCount++;
                    results.add(new AsyncBatchTestOrchestrator.SampleResult(
                            dataSetId, false, errorDetail, null, null, null
                    ));
                    String msg = errorDetail != null ? errorDetail : "Generation failed";
                    batchSseRegistry.send(runId, "sample_done", Map.of(
                            "sampleIndex", i + 1,
                            "success", false,
                            "dataSetExternalId", dataSetId,
                            "errorDetail", msg
                    ));
                }
            }

            CoverageSummaryView coverage = computeCoverage(templateId, session);
            BigDecimal anchorPct = coverageToBigDecimal(coverage, DIMENSION_ANCHOR_BINDINGS);
            BigDecimal variablePct = coverageToBigDecimal(coverage, DIMENSION_REQUIRED_VARIABLES);
            BigDecimal samplePct = coverageToBigDecimal(coverage, DIMENSION_REQUIRED_SAMPLES);

            final int finalSucceededCount = succeededCount;
            final int finalFailedCount = failedCount;
            final boolean allSucceeded = finalFailedCount == 0;
            final boolean gatePassed = coverage != null && allSucceeded && !coverage.belowThreshold();
            final String sampleResultsJson = writeSampleResults(results);

            run.completeRun(
                    finalSucceededCount,
                    finalFailedCount,
                    0,
                    0,
                    sampleResultsJson,
                    anchorPct,
                    variablePct,
                    samplePct,
                    allSucceeded,
                    gatePassed
            );
            batchTestRunRepository.save(run);
            pruneOldRuns(templateId);

            batchSseRegistry.send(runId, "batch_completed", Map.of(
                    "runId", runId.toString(),
                    "successCount", finalSucceededCount,
                    "failedCount", finalFailedCount,
                    "anchorCoveragePct", anchorPct != null ? anchorPct : BigDecimal.ZERO,
                    "variableCoveragePct", variablePct != null ? variablePct : BigDecimal.ZERO,
                    "gatePassed", gatePassed
            ));
        } catch (Exception ex) {
            LOG.error("Batch test run {} failed unexpectedly", runId, ex);
            run.failRun();
            batchTestRunRepository.save(run);
            batchSseRegistry.send(runId, "batch_failed", Map.of(
                    "error", "Batch test run encountered an unexpected error"
            ));
        } finally {
            batchSseRegistry.complete(runId);
        }
    }

    private CoverageSummaryView computeCoverage(UUID templateId, ManagementSessionClaims session) {
        try {
            return templateCoveragePort.compute(templateId, session);
        } catch (Exception ex) {
            LOG.warn("Failed to compute coverage for templateId={}: {}", templateId, ex.getMessage());
            return null;
        }
    }

    private BigDecimal coverageToBigDecimal(CoverageSummaryView coverage, String dimensionName) {
        if (coverage == null) {
            return null;
        }
        return coverage.dimensions().stream()
                .filter(d -> dimensionName.equals(d.dimensionCode()))
                .findFirst()
                .map(d -> BigDecimal.valueOf(d.percentage()).setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private void pruneOldRuns(UUID templateId) {
        List<BatchTestRunEntity> visible = batchTestRunRepository
                .findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(templateId);
        if (visible.size() > maxVisibleRuns) {
            visible.subList(maxVisibleRuns, visible.size()).forEach(old -> {
                old.hide();
                batchTestRunRepository.save(old);
            });
        }
    }

    private String writeSampleResults(List<AsyncBatchTestOrchestrator.SampleResult> results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
