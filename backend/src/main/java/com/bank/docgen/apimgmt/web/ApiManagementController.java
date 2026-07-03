package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ManagementInvocationSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.api.RollbackApiPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.service.ApiManagementService;
import com.bank.docgen.apimgmt.service.ApiPolicyImpactPreviewService;
import com.bank.docgen.apimgmt.service.ApiPolicyRollbackService;
import com.bank.docgen.apimgmt.service.ManagementInvocationQueryService;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
@RequestMapping("/api/management/v1/templates/{templateId}/api")
public class ApiManagementController {

    private final ApiManagementService apiManagementService;
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiPolicyRollbackService apiPolicyRollbackService;
    private final ManagementInvocationQueryService managementInvocationQueryService;
    private final TraceIdProvider traceIdProvider;

    public ApiManagementController(
            ApiManagementService apiManagementService,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiPolicyRollbackService apiPolicyRollbackService,
            ManagementInvocationQueryService managementInvocationQueryService,
            TraceIdProvider traceIdProvider
    ) {
        this.apiManagementService = apiManagementService;
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.apiPolicyRollbackService = apiPolicyRollbackService;
        this.managementInvocationQueryService = managementInvocationQueryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/policy")
    public SuccessEnvelope<ApiPolicyView> getPolicy(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.getPolicy(templateId, session));
    }

    @GetMapping("/contract")
    public SuccessEnvelope<ContractResultView> getCallerContract(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "dev") String environment,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.getCallerContract(templateId, environment, session));
    }

    @GetMapping("/invocations/recent")
    public SuccessEnvelope<List<ManagementInvocationSummaryView>> listRecentInvocations(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, managementInvocationQueryService.listRecentInvocations(templateId, limit, session));
    }

    @PutMapping("/policy")
    public SuccessEnvelope<ApiPolicyView> upsertPolicy(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertApiPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.upsertPolicy(templateId, body, session));
    }

    @PutMapping("/policy/ad-groups")
    public SuccessEnvelope<ApiPolicyView> saveAdGroupsDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveAdGroupsRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveAdGroupsDomain(templateId, body, session));
    }

    @PutMapping("/policy/output")
    public SuccessEnvelope<ApiPolicyView> saveOutputDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveOutputPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveOutputDomain(templateId, body, session));
    }

    @PutMapping("/policy/batch-limits")
    public SuccessEnvelope<ApiPolicyView> saveBatchLimitsDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveBatchLimitsRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveBatchLimitsDomain(templateId, body, session));
    }

    @PutMapping("/policy/encryption")
    public SuccessEnvelope<ApiPolicyView> saveEncryptionDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveEncryptionPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveEncryptionDomain(templateId, body, session));
    }

    @PutMapping("/policy/invocation-retention")
    public SuccessEnvelope<ApiPolicyView> saveInvocationRetentionDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveInvocationRetentionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveInvocationRetentionDomain(templateId, body, session));
    }

    @PutMapping("/policy/default-route")
    public SuccessEnvelope<ApiPolicyView> saveDefaultRouteDomain(
            @PathVariable UUID templateId,
            @Valid @RequestBody SaveDefaultRouteRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.saveDefaultRouteDomain(templateId, body, session));
    }

    @PostMapping("/policy/impact-preview")
    public SuccessEnvelope<ApiPolicyImpactPreviewView> impactPreview(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertApiPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiPolicyImpactPreviewService.preview(templateId, body, session));
    }

    @PostMapping("/policy/rollback/preview")
    public SuccessEnvelope<ApiPolicyImpactPreviewView> rollbackPreview(
            @PathVariable UUID templateId,
            @RequestParam int policyVersion,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiPolicyRollbackService.previewRollback(templateId, policyVersion, session));
    }

    @PostMapping("/policy/rollback")
    public SuccessEnvelope<ApiPolicyView> rollbackPolicy(
            @PathVariable UUID templateId,
            @Valid @RequestBody RollbackApiPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiPolicyRollbackService.rollback(templateId, body, session));
    }

    @GetMapping("/credentials")
    public SuccessEnvelope<List<ApiCredentialSummaryView>> listCredentials(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.listCredentials(templateId, session));
    }

    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<ApiCredentialCreatedView> createCredential(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.createCredential(templateId, session));
    }

    @PostMapping("/credentials/{credentialId}/rotate")
    public SuccessEnvelope<RotateCredentialResponse> rotateCredential(
            @PathVariable UUID templateId,
            @PathVariable UUID credentialId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.rotateCredential(templateId, credentialId, session));
    }

    @PostMapping("/credentials/{credentialId}/revoke")
    public SuccessEnvelope<ApiCredentialSummaryView> revokeCredential(
            @PathVariable UUID templateId,
            @PathVariable UUID credentialId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.revokeCredential(templateId, credentialId, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
