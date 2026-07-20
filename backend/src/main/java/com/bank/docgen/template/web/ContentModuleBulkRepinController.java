package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesRequest;
import com.bank.docgen.template.api.BulkRepinContentModuleReferencesResultView;
import com.bank.docgen.template.service.BulkRepinContentModuleReferencesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/content-module-references")
public class ContentModuleBulkRepinController {

    private final BulkRepinContentModuleReferencesService bulkRepinService;
    private final TraceIdProvider traceIdProvider;

    public ContentModuleBulkRepinController(
            BulkRepinContentModuleReferencesService bulkRepinService,
            TraceIdProvider traceIdProvider
    ) {
        this.bulkRepinService = bulkRepinService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping("/bulk-repin")
    public SuccessEnvelope<BulkRepinContentModuleReferencesResultView> bulkRepin(
            @Valid @RequestBody BulkRepinContentModuleReferencesRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, bulkRepinService.bulkRepin(body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
