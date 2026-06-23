package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.api.AsyncAcceptedResultView;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchGenerateResultView;
import com.bank.docgen.runtime.api.CallableVersionsResultView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.BatchGenerationService;
import com.bank.docgen.runtime.service.RuntimeGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{environment}/v1/templates/{templateExternalId}")
public class RuntimeTemplateController {

    private final TemplateService templateService;
    private final RuntimeGenerationService runtimeGenerationService;
    private final BatchGenerationService batchGenerationService;
    private final TraceIdProvider traceIdProvider;

    public RuntimeTemplateController(
            TemplateService templateService,
            RuntimeGenerationService runtimeGenerationService,
            BatchGenerationService batchGenerationService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateService = templateService;
        this.runtimeGenerationService = runtimeGenerationService;
        this.batchGenerationService = batchGenerationService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/contract")
    public SuccessEnvelope<ContractResultView> getContract(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        ContractResultView result = runtimeGenerationService.getContract(template, session, environment);
        return envelope(request, result);
    }

    @GetMapping("/versions")
    public SuccessEnvelope<CallableVersionsResultView> listVersions(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        CallableVersionsResultView versions = runtimeGenerationService.listCallableVersionsResult(template, environment);
        return envelope(request, versions);
    }

    @PostMapping("/versions/{releaseVersion}/generate")
    public void generateByVersion(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @PathVariable String releaseVersion,
            @Valid @RequestBody GenerateRequestBody body,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws java.io.IOException {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        SyncGenerateResult result = runtimeGenerationService.generateSync(
                template,
                session,
                releaseVersion,
                body
        );
        writeSyncResponse(request, response, templateExternalId, releaseVersion, body, result);
    }

    @PostMapping("/versions/{releaseVersion}/batch-generate")
    public ResponseEntity<SuccessEnvelope<?>> batchGenerateByVersion(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @PathVariable String releaseVersion,
            @Valid @RequestBody BatchGenerateRequestBody body,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        if ("ASYNC_TASK".equalsIgnoreCase(body.output().mode())) {
            AsyncAcceptedResultView result = batchGenerationService.batchGenerateAsync(
                    template,
                    session,
                    releaseVersion,
                    "EXPLICIT",
                    body,
                    environment
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(envelope(request, result));
        }
        BatchGenerateResultView result = batchGenerationService.batchGenerateSync(
                template,
                session,
                releaseVersion,
                "EXPLICIT",
                body
        );
        return ResponseEntity.ok(envelope(request, result));
    }

    @PostMapping("/default/batch-generate")
    public ResponseEntity<SuccessEnvelope<?>> batchGenerateByDefaultRoute(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @Valid @RequestBody BatchGenerateRequestBody body,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        if ("ASYNC_TASK".equalsIgnoreCase(body.output().mode())) {
            AsyncAcceptedResultView result = batchGenerationService.batchGenerateAsync(
                    template,
                    session,
                    null,
                    "DEFAULT",
                    body,
                    environment
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(envelope(request, result));
        }
        BatchGenerateResultView result = batchGenerationService.batchGenerateSync(
                template,
                session,
                null,
                "DEFAULT",
                body
        );
        return ResponseEntity.ok(envelope(request, result));
    }

    @PostMapping("/default/generate")
    public void generateByDefaultRoute(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @Valid @RequestBody GenerateRequestBody body,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws java.io.IOException {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        SyncGenerateResult result = runtimeGenerationService.generateSync(template, session, null, body);
        writeSyncResponse(request, response, templateExternalId, result.resolvedReleaseVersion(), body, result);
    }

    private void writeSyncResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String templateExternalId,
            String releaseVersion,
            GenerateRequestBody body,
            SyncGenerateResult result
    ) throws java.io.IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(result.contentType());
        response.setHeader("auditId", auditId);
        response.setHeader("traceId", traceId);
        response.setHeader("requestId", body.requestId());
        response.setHeader("idempotencyKey", body.idempotencyKey());
        response.setHeader("idempotencyStatus", result.idempotencyStatus());
        response.setHeader("documentId", result.documentId());
        response.setHeader("templateId", templateExternalId);
        response.setHeader("routeType", releaseVersion == null ? "DEFAULT" : "EXPLICIT");
        response.setHeader("resolvedReleaseVersion", result.resolvedReleaseVersion());
        response.setHeader("output.format", body.output().format());
        response.setHeader("output.mode", body.output().mode());
        response.setHeader("fidelityWarningCount", String.valueOf(result.fidelityWarningCodes().size()));
        response.setHeader("fidelityWarningCodes", String.join(",", result.fidelityWarningCodes()));
        response.getOutputStream().write(result.artifactBytes());
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
