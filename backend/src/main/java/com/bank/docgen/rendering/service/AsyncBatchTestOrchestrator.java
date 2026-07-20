package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.AsyncBatchStartResponse;
import com.bank.docgen.rendering.persistence.BatchTestRunEntity;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.port.TemplateCoveragePort;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AsyncBatchTestOrchestrator {

    private static final int MAX_VISIBLE_RUNS = 5;

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final BatchTestRunRepository batchTestRunRepository;
    private final TestDataSetRepository testDataSetRepository;
    private final TemplateRenderContextPort renderContextPort;
    private final SseEmitterRegistry batchSseRegistry;
    private final Executor asyncExecutor;
    private final AsyncBatchTestExecutionSupport execution;

    public AsyncBatchTestOrchestrator(
            TemplatePreviewAuthorizationPort previewAuthorizationPort,
            PreviewGenerationService previewGenerationService,
            BatchTestRunRepository batchTestRunRepository,
            TestDataSetRepository testDataSetRepository,
            TemplateCoveragePort templateCoveragePort,
            TemplateRenderContextPort renderContextPort,
            @Qualifier("batchSseRegistry") SseEmitterRegistry batchSseRegistry,
            ObjectMapper objectMapper,
            @Qualifier("asyncTaskExecutor") Executor asyncExecutor
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.batchTestRunRepository = batchTestRunRepository;
        this.testDataSetRepository = testDataSetRepository;
        this.renderContextPort = renderContextPort;
        this.batchSseRegistry = batchSseRegistry;
        this.asyncExecutor = asyncExecutor;
        this.execution = new AsyncBatchTestExecutionSupport(
                previewGenerationService,
                batchTestRunRepository,
                templateCoveragePort,
                batchSseRegistry,
                objectMapper,
                MAX_VISIBLE_RUNS
        );
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
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        previewAuthorizationPort.requirePreviewAuthor(session);

        var version = renderContextPort.requireInFlightDevVersion(templateId);
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

        asyncExecutor.execute(() -> execution.executeBatchRun(templateId, run, dataSetIds, session));

        return new AsyncBatchStartResponse(runId.toString(), streamUrl);
    }

    /**
     * Creates an SSE emitter for streaming batch test progress.
     */
    public SseEmitter streamProgress(UUID templateId, UUID runId, ManagementSessionClaims session) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        return batchSseRegistry.register(runId);
    }

    /**
     * Canonical async sample shape persisted into {@code sampleResultsJson}
     * (OpenAPI {@code BatchTestHistorySampleResultView} / BDD-PTA-004).
     * Success samples that produced a preview must carry {@code previewId}
     * and stored artifact keys; failure samples may leave those null.
     */
    record SampleResult(
            String dataSetExternalId,
            boolean success,
            String errorDetail,
            String previewId,
            String docxKey,
            String pdfKey
    ) {
    }
}
