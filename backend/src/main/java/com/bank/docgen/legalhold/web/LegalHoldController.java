package com.bank.docgen.legalhold.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.legalhold.api.CreateLegalHoldRequest;
import com.bank.docgen.legalhold.api.LegalHoldView;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.legalhold.service.LegalHoldService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/legal-holds")
public class LegalHoldController {

    private final LegalHoldService legalHoldService;
    private final TraceIdProvider traceIdProvider;

    public LegalHoldController(LegalHoldService legalHoldService, TraceIdProvider traceIdProvider) {
        this.legalHoldService = legalHoldService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<LegalHoldView>> list(
            @RequestParam(required = false) LegalHoldStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null ? 20 : size;
        return envelope(request, legalHoldService.list(status, safePage, safeSize, session));
    }

    @GetMapping("/{id}")
    public SuccessEnvelope<LegalHoldView> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, legalHoldService.get(id, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<LegalHoldView> create(
            @RequestBody CreateLegalHoldRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, legalHoldService.create(body, session));
    }

    @PostMapping("/{id}/release")
    public SuccessEnvelope<LegalHoldView> release(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, legalHoldService.release(id, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
