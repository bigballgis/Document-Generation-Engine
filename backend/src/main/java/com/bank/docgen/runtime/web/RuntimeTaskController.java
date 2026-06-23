package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.api.CancelledTaskResultView;
import com.bank.docgen.runtime.api.TaskQueryResultView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.BatchGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{environment}/v1/templates/{templateExternalId}/tasks")
public class RuntimeTaskController {

    private final TemplateService templateService;
    private final BatchGenerationService batchGenerationService;
    private final TraceIdProvider traceIdProvider;

    public RuntimeTaskController(
            TemplateService templateService,
            BatchGenerationService batchGenerationService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateService = templateService;
        this.batchGenerationService = batchGenerationService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/{taskId}")
    public SuccessEnvelope<TaskQueryResultView> getTask(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @PathVariable String taskId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        TaskQueryResultView result = batchGenerationService.getTask(template, session, taskId, environment);
        return envelope(request, result);
    }

    @PostMapping("/{taskId}/cancel")
    public SuccessEnvelope<CancelledTaskResultView> cancelTask(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @PathVariable String taskId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        CancelledTaskResultView result = batchGenerationService.cancelTask(
                template,
                session,
                taskId,
                environment
        );
        return envelope(request, result);
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
