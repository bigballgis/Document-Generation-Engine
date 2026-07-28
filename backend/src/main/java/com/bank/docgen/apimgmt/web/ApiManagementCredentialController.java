package com.bank.docgen.apimgmt.web;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.service.ApiManagementService;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/api")
public class ApiManagementCredentialController {

    private final ApiManagementService apiManagementService;
    private final ApiManagementWebEnvelopeSupport envelopes;

    public ApiManagementCredentialController(
            ApiManagementService apiManagementService,
            TraceIdProvider traceIdProvider
    ) {
        this.apiManagementService = apiManagementService;
        this.envelopes = new ApiManagementWebEnvelopeSupport(traceIdProvider);
    }

    @GetMapping("/credentials")
    public SuccessEnvelope<List<ApiCredentialSummaryView>> listCredentials(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelopes.envelope(request, apiManagementService.listCredentials(templateId, session));
    }

    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<ApiCredentialCreatedView> createCredential(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            @RequestParam(required = false) Integer expiryDays,
            HttpServletRequest request
    ) {
        return envelopes.envelope(
                request,
                apiManagementService.createCredential(templateId, session, expiryDays)
        );
    }

    @PostMapping("/credentials/{credentialId}/rotate")
    public SuccessEnvelope<RotateCredentialResponse> rotateCredential(
            @PathVariable UUID templateId,
            @PathVariable UUID credentialId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelopes.envelope(
                request,
                apiManagementService.rotateCredential(templateId, credentialId, session)
        );
    }

    @PostMapping("/credentials/{credentialId}/revoke")
    public SuccessEnvelope<ApiCredentialSummaryView> revokeCredential(
            @PathVariable UUID templateId,
            @PathVariable UUID credentialId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelopes.envelope(
                request,
                apiManagementService.revokeCredential(templateId, credentialId, session)
        );
    }
}
