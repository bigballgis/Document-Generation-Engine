package com.bank.docgen.rendering.web;

import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.service.PreviewGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/previews")
public class PreviewController {

    private final PreviewGenerationService previewGenerationService;
    private final TraceIdProvider traceIdProvider;

    public PreviewController(PreviewGenerationService previewGenerationService, TraceIdProvider traceIdProvider) {
        this.previewGenerationService = previewGenerationService;
        this.traceIdProvider = traceIdProvider;
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

    @GetMapping("/{previewId}")
    public SuccessEnvelope<PreviewRecordView> getPreview(
            @PathVariable UUID templateId,
            @PathVariable UUID previewId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, previewGenerationService.getPreview(templateId, previewId, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
