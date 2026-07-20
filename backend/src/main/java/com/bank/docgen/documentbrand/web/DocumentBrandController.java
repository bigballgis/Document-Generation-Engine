package com.bank.docgen.documentbrand.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.documentbrand.api.CreateDocumentBrandRequest;
import com.bank.docgen.documentbrand.api.DocumentBrandView;
import com.bank.docgen.documentbrand.api.UpdateDocumentBrandRequest;
import com.bank.docgen.documentbrand.service.DocumentBrandCatalogService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/document-brands")
public class DocumentBrandController {

    private final DocumentBrandCatalogService catalogService;
    private final TraceIdProvider traceIdProvider;

    public DocumentBrandController(
            DocumentBrandCatalogService catalogService,
            TraceIdProvider traceIdProvider
    ) {
        this.catalogService = catalogService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<DocumentBrandView>> list(
            @RequestParam String groupCode,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.list(session, groupCode, status));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<DocumentBrandView> create(
            @Valid @RequestBody CreateDocumentBrandRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.create(session, body));
    }

    @GetMapping("/{documentBrandCode}")
    public SuccessEnvelope<DocumentBrandView> get(
            @PathVariable String documentBrandCode,
            @RequestParam String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.get(session, groupCode, documentBrandCode));
    }

    @PutMapping("/{documentBrandCode}")
    public SuccessEnvelope<DocumentBrandView> update(
            @PathVariable String documentBrandCode,
            @Valid @RequestBody UpdateDocumentBrandRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.update(session, documentBrandCode, body));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
