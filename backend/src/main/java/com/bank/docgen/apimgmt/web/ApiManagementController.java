package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.service.ApiManagementService;
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
    private final TraceIdProvider traceIdProvider;

    public ApiManagementController(ApiManagementService apiManagementService, TraceIdProvider traceIdProvider) {
        this.apiManagementService = apiManagementService;
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

    @PutMapping("/policy")
    public SuccessEnvelope<ApiPolicyView> upsertPolicy(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertApiPolicyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, apiManagementService.upsertPolicy(templateId, body, session));
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
