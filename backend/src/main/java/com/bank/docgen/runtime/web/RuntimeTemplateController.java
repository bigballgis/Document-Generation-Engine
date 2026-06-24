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
import com.bank.docgen.runtime.service.IdempotencyConstants;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.runtime.service.RuntimeGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.RouteType;
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
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;

    public RuntimeTemplateController(
            TemplateService templateService,
            RuntimeGenerationService runtimeGenerationService,
            BatchGenerationService batchGenerationService,
            TraceIdProvider traceIdProvider,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder
    ) {
        this.templateService = templateService;
        this.runtimeGenerationService = runtimeGenerationService;
        this.batchGenerationService = batchGenerationService;
        this.traceIdProvider = traceIdProvider;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
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
        CallableVersionsResultView versions =
                runtimeGenerationService.listCallableVersionsResult(template, session, environment);
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
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        runtimeGenerationAuditRecorder.recordSyncGeneration(
                template,
                session,
                environment,
                RouteType.EXPLICIT_VERSION,
                result.resolvedReleaseVersion(),
                body.output().format(),
                body.output().mode(),
                body.requestId(),
                body.idempotencyKey(),
                result.idempotencyStatus(),
                result.documentId(),
                IdempotencyConstants.STATUS_REPLAYED.equals(result.idempotencyStatus())
                        ? RuntimeGenerationAuditRecorder.OUTCOME_REPLAYED
                        : RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                traceId
        );
        writeSyncResponse(request, response, templateExternalId, RouteType.EXPLICIT_VERSION, body, result);
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
                    RouteType.EXPLICIT_VERSION,
                    body,
                    environment
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(envelope(request, result));
        }
        BatchGenerateResultView result = batchGenerationService.batchGenerateSync(
                template,
                session,
                environment,
                releaseVersion,
                RouteType.EXPLICIT_VERSION,
                body,
                traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"))
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
                    RouteType.DEFAULT_ROUTE,
                    body,
                    environment
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(envelope(request, result));
        }
        BatchGenerateResultView result = batchGenerationService.batchGenerateSync(
                template,
                session,
                environment,
                null,
                RouteType.DEFAULT_ROUTE,
                body,
                traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"))
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
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        runtimeGenerationAuditRecorder.recordSyncGeneration(
                template,
                session,
                environment,
                RouteType.DEFAULT_ROUTE,
                result.resolvedReleaseVersion(),
                body.output().format(),
                body.output().mode(),
                body.requestId(),
                body.idempotencyKey(),
                result.idempotencyStatus(),
                result.documentId(),
                IdempotencyConstants.STATUS_REPLAYED.equals(result.idempotencyStatus())
                        ? RuntimeGenerationAuditRecorder.OUTCOME_REPLAYED
                        : RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                traceId
        );
        writeSyncResponse(request, response, templateExternalId, RouteType.DEFAULT_ROUTE, body, result);
    }

    private void writeSyncResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String templateExternalId,
            String routeType,
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
        response.setHeader("routeType", routeType);
        response.setHeader("resolvedReleaseVersion", result.resolvedReleaseVersion());
        response.setHeader("output.format", body.output().format());
        response.setHeader("output.mode", body.output().mode());
        response.setHeader("fidelityWarningCount", String.valueOf(result.fidelityWarningCodes().size()));
        response.setHeader("fidelityWarningCodes", String.join(",", result.fidelityWarningCodes()));
        if (result.artifactStream() != null) {
            try (var artifactStream = result.artifactStream()) {
                artifactStream.transferTo(response.getOutputStream());
            }
            return;
        }
        response.getOutputStream().write(result.artifactBytes());
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
