package com.bank.docgen.documentbrand.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.documentbrand.api.CreateLegalEntityRequest;
import com.bank.docgen.documentbrand.api.GroupDefaultLegalEntityView;
import com.bank.docgen.documentbrand.api.LegalEntityView;
import com.bank.docgen.documentbrand.api.PutGroupDefaultLegalEntityRequest;
import com.bank.docgen.documentbrand.api.UpdateLegalEntityRequest;
import com.bank.docgen.documentbrand.service.LegalEntityCatalogService;
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
@RequestMapping("/api/management/v1")
public class LegalEntityController {

    private final LegalEntityCatalogService catalogService;
    private final TraceIdProvider traceIdProvider;

    public LegalEntityController(
            LegalEntityCatalogService catalogService,
            TraceIdProvider traceIdProvider
    ) {
        this.catalogService = catalogService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/legal-entities")
    public SuccessEnvelope<PageView<LegalEntityView>> list(
            @RequestParam String groupCode,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.list(session, groupCode, status));
    }

    @PostMapping("/legal-entities")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<LegalEntityView> create(
            @Valid @RequestBody CreateLegalEntityRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.create(session, body));
    }

    @GetMapping("/legal-entities/{legalEntityCode}")
    public SuccessEnvelope<LegalEntityView> get(
            @PathVariable String legalEntityCode,
            @RequestParam String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.get(session, groupCode, legalEntityCode));
    }

    @PutMapping("/legal-entities/{legalEntityCode}")
    public SuccessEnvelope<LegalEntityView> update(
            @PathVariable String legalEntityCode,
            @Valid @RequestBody UpdateLegalEntityRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.update(session, legalEntityCode, body));
    }

    @GetMapping("/groups/{groupCode}/default-legal-entity")
    public SuccessEnvelope<GroupDefaultLegalEntityView> getDefault(
            @PathVariable String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.getDefault(session, groupCode));
    }

    @PutMapping("/groups/{groupCode}/default-legal-entity")
    public SuccessEnvelope<GroupDefaultLegalEntityView> putDefault(
            @PathVariable String groupCode,
            @Valid @RequestBody PutGroupDefaultLegalEntityRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, catalogService.putDefault(session, groupCode, body));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
