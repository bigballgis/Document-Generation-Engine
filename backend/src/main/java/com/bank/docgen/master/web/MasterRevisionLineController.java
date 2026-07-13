package com.bank.docgen.master.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.master.api.MasterRevisionLineDetailView;
import com.bank.docgen.master.api.MasterRevisionLineSummaryView;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.master.service.MasterRevisionLineService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/masters/{masterId}/revision-lines")
public class MasterRevisionLineController {

    private final MasterRevisionLineService masterRevisionLineService;
    private final TraceIdProvider traceIdProvider;

    public MasterRevisionLineController(
            MasterRevisionLineService masterRevisionLineService,
            TraceIdProvider traceIdProvider
    ) {
        this.masterRevisionLineService = masterRevisionLineService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<MasterRevisionLineSummaryView>> list(
            @PathVariable UUID masterId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterRevisionLineService.list(masterId, page, size, session));
    }

    @GetMapping("/{revisionLineId}")
    public SuccessEnvelope<MasterRevisionLineDetailView> get(
            @PathVariable UUID masterId,
            @PathVariable UUID revisionLineId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterRevisionLineService.get(masterId, revisionLineId, session));
    }

    @GetMapping("/{revisionLineId}/download")
    public void download(
            @PathVariable UUID masterId,
            @PathVariable UUID revisionLineId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws java.io.IOException {
        try (MasterDocumentService.MasterDownloadArtifact artifact =
                     masterRevisionLineService.openDownload(masterId, revisionLineId, session)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(artifact.contentType());
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + sanitizeDownloadFilename(artifact.filename()) + "\""
            );
            artifact.contentStream().transferTo(response.getOutputStream());
        }
    }

    @DeleteMapping("/{revisionLineId}")
    public SuccessEnvelope<Void> delete(
            @PathVariable UUID masterId,
            @PathVariable UUID revisionLineId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        masterRevisionLineService.deleteRevisionLine(masterId, revisionLineId, session);
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), null);
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }

    private String sanitizeDownloadFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "master.docx";
        }
        return filename.replaceAll("[\\r\\n\"]", "_");
    }
}
