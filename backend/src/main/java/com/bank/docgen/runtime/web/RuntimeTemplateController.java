package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.api.CallableVersionsResultView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.InvocationDetailResultView;
import com.bank.docgen.runtime.api.InvocationDetailView;
import com.bank.docgen.runtime.api.InvocationListResultView;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.BatchGenerationService;
import com.bank.docgen.runtime.service.InvocationQueryService;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.runtime.service.RuntimeGenerationService;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.RouteType;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{environment}/v1/templates/{templateExternalId}")
public class RuntimeTemplateController {

    private final TemplateService templateService;
    private final RuntimeGenerationService runtimeGenerationService;
    private final TraceIdProvider traceIdProvider;
    private final InvocationQueryService invocationQueryService;
    private final RuntimeTemplateSyncSupport syncSupport;
    private final RuntimeTemplateBatchSupport batchSupport;

    public RuntimeTemplateController(
            TemplateService templateService,
            RuntimeGenerationService runtimeGenerationService,
            BatchGenerationService batchGenerationService,
            TraceIdProvider traceIdProvider,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            InvocationRecordService invocationRecordService,
            InvocationQueryService invocationQueryService,
            ApiPolicyRepository apiPolicyRepository,
            MessageResolver messageResolver
    ) {
        this.templateService = templateService;
        this.runtimeGenerationService = runtimeGenerationService;
        this.traceIdProvider = traceIdProvider;
        this.invocationQueryService = invocationQueryService;
        this.syncSupport = new RuntimeTemplateSyncSupport(
                invocationRecordService,
                apiPolicyRepository,
                traceIdProvider,
                runtimeGenerationAuditRecorder,
                messageResolver
        );
        this.batchSupport = new RuntimeTemplateBatchSupport(batchGenerationService, traceIdProvider);
    }

    @GetMapping("/invocations")
    public SuccessEnvelope<InvocationListResultView> listInvocations(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @RequestParam(required = false, defaultValue = "logical") String view,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        InvocationListResultView result = invocationQueryService.listInvocations(
                template,
                session,
                view,
                requestId,
                page,
                size
        );
        return envelope(request, result);
    }

    @GetMapping("/invocations/{invocationId}")
    public SuccessEnvelope<InvocationDetailResultView> getInvocation(
            @PathVariable String environment,
            @PathVariable String templateExternalId,
            @PathVariable String invocationId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId);
        InvocationDetailView detail = invocationQueryService.getInvocationDetail(
                template,
                session,
                invocationId
        );
        return envelope(request, new InvocationDetailResultView(detail));
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
        try {
            SyncGenerateResult result = runtimeGenerationService.generateSync(
                    template,
                    session,
                    releaseVersion,
                    body
            );
            syncSupport.auditRecordAndWrite(
                    template,
                    session,
                    environment,
                    RouteType.EXPLICIT_VERSION,
                    releaseVersion,
                    templateExternalId,
                    body,
                    result,
                    request,
                    response
            );
        } catch (RuntimeException ex) {
            syncSupport.recordFailedSingleInvocation(
                    template,
                    session,
                    environment,
                    RouteType.EXPLICIT_VERSION,
                    releaseVersion,
                    body,
                    ex
            );
            throw ex;
        }
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
        return batchSupport.batchGenerate(
                template, session, environment, releaseVersion, RouteType.EXPLICIT_VERSION, body, request);
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
        return batchSupport.batchGenerate(
                template, session, environment, null, RouteType.DEFAULT_ROUTE, body, request);
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
        try {
            SyncGenerateResult result = runtimeGenerationService.generateSync(template, session, null, body);
            syncSupport.auditRecordAndWrite(
                    template,
                    session,
                    environment,
                    RouteType.DEFAULT_ROUTE,
                    null,
                    templateExternalId,
                    body,
                    result,
                    request,
                    response
            );
        } catch (RuntimeException ex) {
            syncSupport.recordFailedSingleInvocation(
                    template,
                    session,
                    environment,
                    RouteType.DEFAULT_ROUTE,
                    null,
                    body,
                    ex
            );
            throw ex;
        }
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
