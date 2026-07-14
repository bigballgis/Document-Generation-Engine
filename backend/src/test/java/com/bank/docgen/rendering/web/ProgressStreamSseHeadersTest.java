package com.bank.docgen.rendering.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.rendering.service.AsyncBatchTestOrchestrator;
import com.bank.docgen.rendering.service.AsyncPreviewOrchestrator;
import com.bank.docgen.rendering.service.BatchTestHistoryService;
import com.bank.docgen.rendering.service.BatchTestGenerationService;
import com.bank.docgen.rendering.service.PreviewArtifactDownloadService;
import com.bank.docgen.rendering.service.FidelityWarningViewedService;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.rendering.service.SubmitTestEligibilityService;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * LR-B3: progress-stream responses must disable proxy buffering and caching so SSE
 * frames reach the client incrementally through nginx (CD-PIT-12).
 */
@ExtendWith(MockitoExtension.class)
class ProgressStreamSseHeadersTest {

    @Mock
    private PreviewGenerationService previewGenerationService;
    @Mock
    private BatchTestGenerationService batchTestGenerationService;
    @Mock
    private PreviewArtifactDownloadService previewArtifactDownloadService;
    @Mock
    private AsyncPreviewOrchestrator asyncPreviewOrchestrator;
    @Mock
    private FidelityWarningViewedService fidelityWarningViewedService;
    @Mock
    private AsyncBatchTestOrchestrator batchTestOrchestrator;
    @Mock
    private BatchTestHistoryService batchTestHistoryService;
    @Mock
    private SubmitTestEligibilityService eligibilityService;

    private final TraceIdProvider traceIdProvider = new TraceIdProvider();

    @Test
    void previewProgressStreamSetsAntiBufferingHeaders() {
        UUID templateId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        when(asyncPreviewOrchestrator.streamProgress(eq(templateId), eq(previewId), any()))
                .thenReturn(emitter);
        PreviewController controller = new PreviewController(
                previewGenerationService,
                batchTestGenerationService,
                previewArtifactDownloadService,
                asyncPreviewOrchestrator,
                fidelityWarningViewedService,
                traceIdProvider
        );

        ResponseEntity<SseEmitter> response = controller.streamPreviewProgress(templateId, previewId, null);

        assertThat(response.getBody()).isSameAs(emitter);
        assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-cache");
    }

    @Test
    void batchProgressStreamSetsAntiBufferingHeaders() {
        UUID templateId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        SseEmitter emitter = new SseEmitter();
        when(batchTestOrchestrator.streamProgress(eq(templateId), eq(runId), any()))
                .thenReturn(emitter);
        BatchTestController controller = new BatchTestController(
                batchTestOrchestrator,
                batchTestHistoryService,
                eligibilityService,
                traceIdProvider
        );

        ResponseEntity<SseEmitter> response = controller.streamBatchProgress(templateId, runId, null);

        assertThat(response.getBody()).isSameAs(emitter);
        assertThat(response.getHeaders().getFirst("X-Accel-Buffering")).isEqualTo("no");
        assertThat(response.getHeaders().getFirst("Cache-Control")).isEqualTo("no-cache");
    }
}
