package com.bank.docgen.rendering.web;

import com.bank.docgen.rendering.api.AsyncPreviewStartResponse;
import com.bank.docgen.rendering.api.BatchTestGenerateRequest;
import com.bank.docgen.rendering.api.BatchTestSummaryView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.PreviewSummaryView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.service.AsyncPreviewOrchestrator;
import com.bank.docgen.rendering.service.BatchTestGenerationService;
import com.bank.docgen.rendering.service.PreviewArtifactDownloadService;
import com.bank.docgen.rendering.service.PreviewArtifactDownloadService.PreviewArtifactFormat;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/previews")
public class PreviewController {

    private final PreviewGenerationService previewGenerationService;
    private final BatchTestGenerationService batchTestGenerationService;
    private final PreviewArtifactDownloadService previewArtifactDownloadService;
    private final AsyncPreviewOrchestrator asyncPreviewOrchestrator;
    private final TraceIdProvider traceIdProvider;

    public PreviewController(
            PreviewGenerationService previewGenerationService,
            BatchTestGenerationService batchTestGenerationService,
            PreviewArtifactDownloadService previewArtifactDownloadService,
            AsyncPreviewOrchestrator asyncPreviewOrchestrator,
            TraceIdProvider traceIdProvider
    ) {
        this.previewGenerationService = previewGenerationService;
        this.batchTestGenerationService = batchTestGenerationService;
        this.previewArtifactDownloadService = previewArtifactDownloadService;
        this.asyncPreviewOrchestrator = asyncPreviewOrchestrator;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<PreviewSummaryView>> listPreviews(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, previewGenerationService.listPreviews(templateId, session));
    }

    @PostMapping("/test-generate")
    public SuccessEnvelope<PreviewRecordView> testGenerate(
            @PathVariable UUID templateId,
            @Valid @RequestBody TestGenerateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        PreviewRecordView result = previewGenerationService.testGenerate(templateId, body, session);
        return envelope(request, result);
    }

    @PostMapping("/batch-test")
    public SuccessEnvelope<BatchTestSummaryView> batchTest(
            @PathVariable UUID templateId,
            @Valid @RequestBody BatchTestGenerateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        BatchTestSummaryView result = batchTestGenerationService.runBatch(templateId, body, session);
        return envelope(request, result);
    }

    @GetMapping("/{previewId}")
    public SuccessEnvelope<PreviewRecordView> getPreview(
            @PathVariable UUID templateId,
            @PathVariable UUID previewId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, previewGenerationService.getPreview(templateId, previewId, session));
    }

    @GetMapping("/{previewId}/artifacts/docx")
    public void downloadDocx(
            @PathVariable UUID templateId,
            @PathVariable UUID previewId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws IOException {
        streamArtifact(templateId, previewId, PreviewArtifactFormat.DOCX, session, response);
    }

    @GetMapping("/{previewId}/artifacts/pdf")
    public void downloadPdf(
            @PathVariable UUID templateId,
            @PathVariable UUID previewId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws IOException {
        streamArtifact(templateId, previewId, PreviewArtifactFormat.PDF, session, response);
    }

    private void streamArtifact(
            UUID templateId,
            UUID previewId,
            PreviewArtifactFormat format,
            ManagementSessionClaims session,
            HttpServletResponse response
    ) throws IOException {
        try (PreviewArtifactDownloadService.PreviewDownloadArtifact artifact =
                previewArtifactDownloadService.openDownload(templateId, previewId, format, session)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(artifact.contentType());
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + sanitizeDownloadFilename(artifact.filename()) + "\""
            );
            artifact.contentStream().transferTo(response.getOutputStream());
        }
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }

    @PostMapping("/async-preview")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SuccessEnvelope<AsyncPreviewStartResponse> startAsyncPreview(
            @PathVariable UUID templateId,
            @Valid @RequestBody TestGenerateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        // Relative stream URL so the browser stays same-origin via the frontend proxy
        // (absolute host:port from the servlet request points at the backend container port
        // and breaks SSE fetch with CORS / unreachable host from the UI origin).
        String baseUrl = "/api/management/v1/templates/" + templateId + "/previews";
        AsyncPreviewStartResponse response = asyncPreviewOrchestrator.startAsync(templateId, body, session, baseUrl);
        return envelope(request, response);
    }

    @GetMapping(value = "/{previewId}/progress-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamPreviewProgress(
            @PathVariable UUID templateId,
            @PathVariable UUID previewId,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        return SseResponseHeaders.withAntiBufferingHeaders(
                asyncPreviewOrchestrator.streamProgress(templateId, previewId, session));
    }

    private String sanitizeDownloadFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "preview.docx";
        }
        return filename.replaceAll("[\\r\\n\"]", "_");
    }
}
