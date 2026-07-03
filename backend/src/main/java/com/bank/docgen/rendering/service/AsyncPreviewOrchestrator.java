package com.bank.docgen.rendering.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.rendering.api.AsyncPreviewStartResponse;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.service.TemplateAccessDeniedException;
import com.bank.docgen.template.service.TemplateService;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SuppressWarnings("PMD.ExcessiveImports")

@Service
public class AsyncPreviewOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncPreviewOrchestrator.class);
    private static final Duration TEMP_TTL = Duration.ofHours(24);
    private static final String BATCH_TEST_STORAGE_PREFIX = "batch-test/";
    static final String TEMP_STORAGE_PREFIX = "preview-temp/";

    private final TemplateService templateService;
    private final GroupAccessService groupAccessService;
    private final PreviewGenerationService previewGenerationService;
    private final PreviewRecordRepository previewRecordRepository;
    private final PreviewConcurrencyGuard concurrencyGuard;
    private final SseEmitterRegistry sseRegistry;
    private final Executor asyncExecutor;

    public AsyncPreviewOrchestrator(
            TemplateService templateService,
            GroupAccessService groupAccessService,
            PreviewGenerationService previewGenerationService,
            PreviewRecordRepository previewRecordRepository,
            PreviewConcurrencyGuard concurrencyGuard,
            @Qualifier("previewSseRegistry") SseEmitterRegistry sseRegistry,
            @Qualifier("asyncTaskExecutor") Executor asyncExecutor
    ) {
        this.templateService = templateService;
        this.groupAccessService = groupAccessService;
        this.previewGenerationService = previewGenerationService;
        this.previewRecordRepository = previewRecordRepository;
        this.concurrencyGuard = concurrencyGuard;
        this.sseRegistry = sseRegistry;
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * Starts an async preview generation. Returns immediately with previewId and SSE stream URL.
     * Throws {@link PreviewConcurrencyLimitException} if the limit is reached.
     */
    @Transactional
    public AsyncPreviewStartResponse startAsync(
            UUID templateId,
            TestGenerateRequest request,
            ManagementSessionClaims session,
            String baseUrl
    ) {
        templateService.requireReadableTemplate(templateId, session);
        if (!groupAccessService.canAuthorTemplates(session)) {
            throw new TemplateAccessDeniedException();
        }
        if (!concurrencyGuard.tryAcquire()) {
            throw new PreviewConcurrencyLimitException();
        }

        UUID previewId = UUID.randomUUID();
        String streamUrl = baseUrl + "/" + previewId + "/progress-stream";

        asyncExecutor.execute(() -> runGeneration(templateId, previewId, request, session));

        return new AsyncPreviewStartResponse(previewId.toString(), streamUrl);
    }

    /**
     * Creates an SSE emitter for streaming preview progress.
     * If generation is already complete, sends the final event immediately.
     */
    public SseEmitter streamProgress(UUID templateId, UUID previewId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        SseEmitter emitter = sseRegistry.register(previewId);

        previewRecordRepository.findById(previewId).ifPresent(record -> {
            if (record.getStatus() == PreviewStatus.SUCCEEDED
                    || record.getStatus() == PreviewStatus.FAILED
                    || record.getStatus() == PreviewStatus.EXPIRED) {
                sendFinalEvent(previewId, record);
                sseRegistry.complete(previewId);
            }
        });

        return emitter;
    }

    private void runGeneration(
            UUID templateId,
            UUID previewId,
            TestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        try {
            sseRegistry.send(previewId, "progress", Map.of(
                    "stage", "GENERATING_DOCX",
                    "percent", 10,
                    "message", "Starting preview generation"
            ));

            var preview = previewGenerationService.testGenerate(templateId, request, session);

            previewRecordRepository.findById(previewId).ifPresent(record -> {
                if (record.getStatus() == PreviewStatus.SUCCEEDED) {
                    record.setExpiresAt(Instant.now().plus(TEMP_TTL));
                    if (record.getArtifactStorageKey() != null) {
                        record.setTempStorageKey(record.getArtifactStorageKey());
                    }
                    previewRecordRepository.save(record);
                }
            });

            if (preview.status() == PreviewStatus.SUCCEEDED) {
                sseRegistry.send(previewId, "completed", Map.of(
                        "previewId", previewId.toString(),
                        "docxDownloadUrl", buildDownloadUrl(templateId, previewId, "docx"),
                        "pdfDownloadUrl", buildDownloadUrl(templateId, previewId, "pdf"),
                        "expiresAt", Instant.now().plus(TEMP_TTL).toString()
                ));
            } else {
                sseRegistry.send(previewId, "failed", Map.of(
                        "error", "Document generation failed",
                        "retryable", true
                ));
            }
        } catch (PreviewGenerationException ex) {
            LOG.warn("Async preview generation failed for previewId={}: {}", previewId, ex.getMessage());
            sseRegistry.send(previewId, "failed", Map.of(
                    "error", ex.messageKey(),
                    "retryable", true
            ));
        } catch (Exception ex) {
            LOG.error("Unexpected error during async preview generation for previewId={}", previewId, ex);
            sseRegistry.send(previewId, "failed", Map.of(
                    "error", "Internal error during generation",
                    "retryable", true
            ));
        } finally {
            concurrencyGuard.release();
            sseRegistry.complete(previewId);
        }
    }

    private void sendFinalEvent(UUID previewId, PreviewRecordEntity record) {
        if (record.getStatus() == PreviewStatus.SUCCEEDED) {
            UUID templateId = record.getTemplateId();
            sseRegistry.send(previewId, "completed", Map.of(
                    "previewId", previewId.toString(),
                    "docxDownloadUrl", buildDownloadUrl(templateId, previewId, "docx"),
                    "pdfDownloadUrl", buildDownloadUrl(templateId, previewId, "pdf"),
                    "expiresAt", record.getExpiresAt() != null
                            ? record.getExpiresAt().toString()
                            : Instant.now().plus(TEMP_TTL).toString()
            ));
        } else {
            sseRegistry.send(previewId, "failed", Map.of(
                    "error", record.getErrorDetails() != null ? record.getErrorDetails() : "Generation failed",
                    "retryable", true
            ));
        }
    }

    private String buildDownloadUrl(UUID templateId, UUID previewId, String format) {
        return "/api/management/v1/templates/" + templateId
                + "/previews/" + previewId + "/artifacts/" + format;
    }
}
