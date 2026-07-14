package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.OutdatedClauseReferenceAuthorTaskView;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/author-workflow")
public class TemplateAuthorWorkflowController {

    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final TraceIdProvider traceIdProvider;

    public TemplateAuthorWorkflowController(
            TemplateContentModuleReferenceService contentModuleReferenceService,
            TraceIdProvider traceIdProvider
    ) {
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/outdated-clause-references")
    public SuccessEnvelope<List<OutdatedClauseReferenceAuthorTaskView>> listOutdatedClauseReferenceTasks(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(
                request,
                contentModuleReferenceService.listOutdatedClauseReferenceAuthorTasks(session)
        );
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
