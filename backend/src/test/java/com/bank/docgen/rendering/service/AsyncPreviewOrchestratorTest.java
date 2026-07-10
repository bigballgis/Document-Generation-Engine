package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.rendering.api.AsyncPreviewStartResponse;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncPreviewOrchestratorTest {

    @Mock
    private TemplatePreviewAuthorizationPort previewAuthorizationPort;
    @Mock
    private PreviewGenerationService previewGenerationService;
    @Mock
    private PreviewRecordRepository previewRecordRepository;

    private PreviewConcurrencyGuard concurrencyGuard;
    private SseEmitterRegistry sseRegistry;
    private AsyncPreviewOrchestrator orchestrator;
    private UUID templateId;
    private ManagementSessionClaims session;

    /**
     * Synchronous executor for deterministic testing.
     */
    private final Executor syncExecutor = Runnable::run;

    @BeforeEach
    void setUp() {
        concurrencyGuard = new PreviewConcurrencyGuard(3);
        sseRegistry = new SseEmitterRegistry();
        orchestrator = new AsyncPreviewOrchestrator(
                previewAuthorizationPort, previewGenerationService,
                previewRecordRepository, concurrencyGuard, sseRegistry, syncExecutor
        );
        templateId = UUID.randomUUID();
        session = new ManagementSessionClaims(
                "10000001", "Author", "author@test.com",
                AuthSource.LOCAL, List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"), "route.home", List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
        UUID masterId = UUID.randomUUID();
        when(previewAuthorizationPort.requireReadableSnapshot(templateId, session))
                .thenReturn(new RenderableTemplateSnapshot(templateId, masterId, "RETAIL"));
    }

    @Test
    void startAsync_withCapacity_returnsPreviewIdAndStreamUrl() {
        stubSuccessfulPreview();

        AsyncPreviewStartResponse response = orchestrator.startAsync(
                templateId, new TestGenerateRequest(null, "TDS-001"), session, "http://localhost/api/management/v1/templates/" + templateId + "/previews"
        );

        assertThat(response.previewId()).isNotBlank();
        assertThat(response.streamUrl()).contains("/progress-stream");
    }

    @Test
    void startAsync_concurrencyLimitReached_throwsException() {
        // Exhaust all slots
        concurrencyGuard.tryAcquire();
        concurrencyGuard.tryAcquire();
        concurrencyGuard.tryAcquire();

        assertThatThrownBy(() -> orchestrator.startAsync(
                templateId, new TestGenerateRequest(null, "TDS-001"), session, "http://localhost"
        )).isInstanceOf(PreviewConcurrencyLimitException.class);

        verify(previewGenerationService, never()).testGenerate(any(), any(), any(UUID.class), any());
    }

    @Test
    void startAsync_releasesSlotAfterGeneration() {
        stubSuccessfulPreview();
        assertThat(concurrencyGuard.getActiveCount()).isZero();

        orchestrator.startAsync(
                templateId, new TestGenerateRequest(null, "TDS-001"), session, "http://localhost"
        );

        // Synchronous executor means generation already ran
        assertThat(concurrencyGuard.getActiveCount()).isZero();
    }

    @Test
    void startAsync_generationFails_releasesSlot() {
        when(previewGenerationService.testGenerate(eq(templateId), any(), any(UUID.class), eq(session)))
                .thenThrow(new PreviewGenerationException("api.error.rendering.generationFailed", new RuntimeException()));

        orchestrator.startAsync(
                templateId, new TestGenerateRequest(null, "TDS-001"), session, "http://localhost"
        );

        assertThat(concurrencyGuard.getActiveCount()).isZero();
    }

    @Test
    void startAsync_passesOrchestratorPreviewIdToGeneration() {
        stubSuccessfulPreview();

        AsyncPreviewStartResponse response = orchestrator.startAsync(
                templateId,
                new TestGenerateRequest(null, "TDS-001"),
                session,
                "http://localhost/api/management/v1/templates/" + templateId + "/previews"
        );

        UUID allocatedId = UUID.fromString(response.previewId());
        verify(previewGenerationService).testGenerate(
                eq(templateId), any(TestGenerateRequest.class), eq(allocatedId), eq(session));
        assertThat(response.streamUrl()).contains(allocatedId.toString());
    }

    private void stubSuccessfulPreview() {
        UUID previewId = UUID.randomUUID();
        PreviewRecordView view = new PreviewRecordView(
                previewId.toString(), templateId.toString(), UUID.randomUUID().toString(),
                PreviewStatus.SUCCEEDED, "DOCX", "rp-v1",
                "previews/" + previewId + "/output.docx",
                "previews/" + previewId + "/output.pdf",
                List.of(),
                new PreviewComparisonView(0, 0, 0, List.of()),
                "TDS-001",
                Instant.now()
        );
        when(previewGenerationService.testGenerate(eq(templateId), any(), any(UUID.class), eq(session)))
                .thenReturn(view);
        when(previewRecordRepository.findById(any())).thenReturn(Optional.empty());
    }
}
