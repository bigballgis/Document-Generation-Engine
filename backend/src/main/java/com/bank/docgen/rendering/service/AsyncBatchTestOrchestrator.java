package com.bank.docgen.rendering.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.api.AsyncBatchStartResponse;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.domain.BatchTestRunStatus;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CoverageDimensionView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CoverageThresholdView;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.service.CoverageComputationService;
import com.bank.docgen.template.service.CoverageThresholdResolver;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AsyncBatchTestOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncBatchTestOrchestrator.class);
    private static final int MAX_VISIBLE_RUNS = 5;
    private static final String BATCH_STORAGE_PREFIX = "batch-test/";

    private final TemplateService templateService;
    private final GroupAccessService groupAccessService;
    private final PreviewGenerationService previewGenerationService;
    private final BatchTestRunRepository batchTestRunRepository;
    private final TestDataSetRepository testDataSetRepository;
    private final CoverageComputationService coverageComputationService;
    private final CoverageThresholdResolver coverageThresholdResolver;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final SseEmitterRegistry batchSseRegistry;
    private final ObjectMapper objectMapper;
    private final Executor asyncExecutor;

    public AsyncBatchTestOrchestrator(
            TemplateService templateService,
            GroupAccessService groupAccessService,
            PreviewGenerationService previewGenerationService,
            BatchTestRunRepository batchTestRunRepository,
            TestDataSetRepository testDataSetRepository,
            CoverageComputationService coverageComputationService,
            CoverageThresholdResolver coverageThresholdResolver,
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            @Qualifier("batchSseRegistry") SseEmitterRegistry batchSseRegistry,
            ObjectMapper objectMapper,
            @Qualifier("asyncTaskExecutor") Executor asyncExecutor
    ) {
        this.templateService = templateService;
        this.groupAccessService = groupAccessService;
        this.previewGenerationService = previewGenerationService;
        this.batchTestRunRepository = batchTestRunRepository;
        this.testDataSetRepository = testDataSetRepository;
        this.coverageComputationService = coverageComputationService;
        this.coverageThresholdResolver = coverageThresholdResolver;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.batchSseRegistry = batchSseRegistry;
        this.objectMapper = objectMapper;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Starts an async batch test run and returns immediately with runId + SSE stream URL.
     */
    @Transactional
    public AsyncBatchStartResponse startBatchRun(
            UUID templateId,
            ManagementSessionClaims session,
            String baseUrl
    ) {
        templateService.requireReadableTemplate(templateId, session);
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }

        var version = templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
        List<TestDataSetEntity> dataSets =
                testDataSetRepository.findByTemplateIdOrderByUpdatedAtDesc(templateId);

        UUID runId = UUID.randomUUID();
        BatchTestRunEntity run = BatchTestRunEntity.startNew(
                runId, templateId, version.getId(), session.username(), dataSets.size()
        );
        batchTestRunRepository.save(run);

        String streamUrl = baseUrl + "/" + runId + "/progress-stream";
        List<String> dataSetIds = dataSets.stream()
                .map(TestDataSetEntity::getExternalId)
                .toList();

        asyncExecutor.execute(() -> executeBatchRun(templateId, runId, dataSetIds, session));

        return new AsyncBatchStartResponse(runId.toString(), streamUrl);
    }

    /**
     * Creates an SSE emitter for streaming batch test progress.
     */
    public SseEmitter streamProgress(UUID templateId, UUID runId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        return batchSseRegistry.register(runId);
    }

    private void executeBatchRun(
            UUID templateId,
            UUID runId,
            List<String> dataSetIds,
            ManagementSessionClaims session
    ) {
        try {
            int total = dataSetIds.size();
            List<SampleResult> results = new ArrayList<>();
            int succeededCount = 0;
            int failedCount = 0;

            for (int i = 0; i < dataSetIds.size(); i++) {
                String dataSetId = dataSetIds.get(i);

                batchSseRegistry.send(runId, "sample_started", Map.of(
                        "sampleIndex", i + 1,
                        "totalSamples", total,
                        "dataSetExternalId", dataSetId
                ));

                PreviewRecordView preview;
                boolean success;
                String errorDetail = null;

                try {
                    preview = previewGenerationService.runTestGenerateForBatch(
                            templateId, dataSetId, runId, session
                    );
                    success = preview.status() == PreviewStatus.SUCCEEDED;
                } catch (Exception ex) {
                    LOG.warn("Batch sample {} failed: {}", dataSetId, ex.getMessage());
                    success = false;
                    errorDetail = ex.getMessage();
                    preview = null;
                }

                if (success) {
                    succeededCount++;
                    results.add(new SampleResult(dataSetId, true, null, null, null));
                    batchSseRegistry.send(runId, "sample_done", Map.of(
                            "sampleIndex", i + 1,
                            "success", true,
                            "dataSetExternalId", dataSetId
                    ));
                } else {
                    failedCount++;
                    results.add(new SampleResult(dataSetId, false, errorDetail, null, null));
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
            CoverageThresholdView threshold = resolveThreshold(templateId, session);

            BigDecimal anchorPct = coverageToBigDecimal(coverage, CoverageComputationService.DIMENSION_ANCHOR_BINDINGS);
            BigDecimal variablePct = coverageToBigDecimal(coverage, CoverageComputationService.DIMENSION_REQUIRED_VARIABLES);
            BigDecimal samplePct = coverageToBigDecimal(coverage, CoverageComputationService.DIMENSION_REQUIRED_SAMPLES);

            final int finalSucceededCount = succeededCount;
            final int finalFailedCount = failedCount;
            final boolean allSucceeded = finalFailedCount == 0;
            final boolean gatePassed = coverage != null && allSucceeded && !coverage.belowThreshold();
            final String sampleResultsJson = writeSampleResults(results);

            batchTestRunRepository.findById(runId).ifPresent(run -> {
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
            });

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
            batchTestRunRepository.findById(runId).ifPresent(run -> {
                run.failRun();
                batchTestRunRepository.save(run);
            });
            batchSseRegistry.send(runId, "batch_failed", Map.of(
                    "error", "Batch test run encountered an unexpected error"
            ));
        } finally {
            batchSseRegistry.complete(runId);
        }
    }

    private CoverageSummaryView computeCoverage(UUID templateId, ManagementSessionClaims session) {
        try {
            return coverageComputationService.compute(templateId, session);
        } catch (Exception ex) {
            LOG.warn("Failed to compute coverage for templateId={}: {}", templateId, ex.getMessage());
            return null;
        }
    }

    private CoverageThresholdView resolveThreshold(UUID templateId, ManagementSessionClaims session) {
        try {
            var template = templateService.requireReadableTemplate(templateId, session);
            return coverageThresholdResolver.resolveForTemplate(template);
        } catch (Exception ex) {
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
        if (visible.size() > MAX_VISIBLE_RUNS) {
            visible.subList(MAX_VISIBLE_RUNS, visible.size()).forEach(old -> {
                old.hide();
                batchTestRunRepository.save(old);
            });
        }
    }

    private String writeSampleResults(List<SampleResult> results) {
        try {
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    record SampleResult(
            String dataSetExternalId,
            boolean success,
            String errorDetail,
            String docxKey,
            String pdfKey
    ) {
    }
}
