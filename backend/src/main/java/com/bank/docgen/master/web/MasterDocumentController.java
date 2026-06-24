package com.bank.docgen.master.web;

import com.bank.docgen.master.api.CreateMasterRequest;
import com.bank.docgen.master.api.DecideMasterReviewRequest;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.master.api.MasterDocumentSummaryView;
import com.bank.docgen.master.api.MasterImpactAnalysisView;
import com.bank.docgen.master.api.SubmitMasterReviewRequest;
import com.bank.docgen.master.api.UpdateMasterRequest;
import com.bank.docgen.master.service.MasterDocumentService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.master.service.MasterDocumentService.MasterDownloadArtifact;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/management/v1/masters")
public class MasterDocumentController {

    private final MasterDocumentService masterDocumentService;
    private final TraceIdProvider traceIdProvider;

    public MasterDocumentController(MasterDocumentService masterDocumentService, TraceIdProvider traceIdProvider) {
        this.masterDocumentService = masterDocumentService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<MasterDocumentSummaryView>> list(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.list(session));
    }

    @GetMapping("/{masterId}")
    public SuccessEnvelope<MasterDocumentDetailView> get(
            @PathVariable UUID masterId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.get(masterId, session));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessEnvelope<MasterDocumentDetailView> create(
            @RequestParam String groupCode,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        CreateMasterRequest metadata = new CreateMasterRequest(groupCode, name, description);
        return envelope(request, masterDocumentService.create(metadata, file, session));
    }

    @PatchMapping("/{masterId}")
    public SuccessEnvelope<MasterDocumentDetailView> update(
            @PathVariable UUID masterId,
            @Valid @RequestBody UpdateMasterRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.updateMetadata(masterId, body, session));
    }

    @PostMapping("/{masterId}/submit-review")
    public SuccessEnvelope<MasterDocumentDetailView> submitReview(
            @PathVariable UUID masterId,
            @Valid @RequestBody SubmitMasterReviewRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.submitReview(masterId, body, session));
    }

    @PostMapping("/{masterId}/review")
    public SuccessEnvelope<MasterDocumentDetailView> decideReview(
            @PathVariable UUID masterId,
            @Valid @RequestBody DecideMasterReviewRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.decideReview(masterId, body, session));
    }

    @GetMapping("/{masterId}/download")
    public void download(
            @PathVariable UUID masterId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws java.io.IOException {
        try (MasterDownloadArtifact artifact = masterDocumentService.openDownload(masterId, session)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(artifact.contentType());
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + sanitizeDownloadFilename(artifact.filename()) + "\""
            );
            artifact.contentStream().transferTo(response.getOutputStream());
        }
    }

    @PutMapping(value = "/{masterId}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessEnvelope<MasterDocumentDetailView> replaceFile(
            @PathVariable UUID masterId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.replaceFile(masterId, file, session));
    }

    @GetMapping("/{masterId}/impact-analysis")
    public SuccessEnvelope<MasterImpactAnalysisView> impactAnalysis(
            @PathVariable UUID masterId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, masterDocumentService.impactAnalysis(masterId, session));
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
