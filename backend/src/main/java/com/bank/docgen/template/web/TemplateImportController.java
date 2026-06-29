package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.service.TemplateImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateImportController {

    private final TemplateImportService templateImportService;
    private final TraceIdProvider traceIdProvider;

    public TemplateImportController(
            TemplateImportService templateImportService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateImportService = templateImportService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<TemplateImportResult> importBundle(
            @Valid @RequestBody ImportTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateImportService.importBundle(body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
