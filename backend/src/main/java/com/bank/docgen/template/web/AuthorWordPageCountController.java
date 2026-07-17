package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AuthorWordPageCountView;
import com.bank.docgen.template.api.UpdateAuthorWordPageCountRequest;
import com.bank.docgen.template.service.TemplateVersionLineService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ADR-0042 / BDD-PRR-C01-008: manage author-declared Word page count on the in-flight DEV version.
 */
@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/dev-version/author-word-page-count")
public class AuthorWordPageCountController {

    private final TemplateVersionLineService templateVersionLineService;
    private final TraceIdProvider traceIdProvider;

    public AuthorWordPageCountController(
            TemplateVersionLineService templateVersionLineService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateVersionLineService = templateVersionLineService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<AuthorWordPageCountView> get(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateVersionLineService.getAuthorWordPageCount(templateId, session));
    }

    @PutMapping
    public SuccessEnvelope<AuthorWordPageCountView> put(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateAuthorWordPageCountRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(
                request,
                templateVersionLineService.updateAuthorWordPageCount(templateId, body, session)
        );
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
