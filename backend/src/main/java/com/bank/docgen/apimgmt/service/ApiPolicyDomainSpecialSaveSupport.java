package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

/**
 * Package-private retention / default-route domain saves (non-standard preview gates).
 */
final class ApiPolicyDomainSpecialSaveSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ObjectMapper objectMapper;
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiManagementAccessSupport access;
    private final ApiPolicyDomainSaveExecutorSupport executor;

    ApiPolicyDomainSpecialSaveSupport(
            ApiPolicyRepository apiPolicyRepository,
            ObjectMapper objectMapper,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiManagementAccessSupport access,
            ApiPolicyDomainSaveExecutorSupport executor
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.objectMapper = objectMapper;
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.access = access;
        this.executor = executor;
    }

    ApiPolicyView saveInvocationRetentionDomain(
            UUID templateId,
            SaveInvocationRetentionRequest request,
            ManagementSessionClaims session
    ) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        ApiPolicyRetentionValidator.validate(
                request.saveGeneratedDocuments(),
                request.invocationRecordRetentionDays(),
                request.documentRetentionDays()
        );
        if (ApiPolicyDomainSaveExecutorSupport.retentionChanged(policy, request) && !request.confirmed()) {
            throw new TemplateValidationException("api.error.apimgmt.policyImpactConfirmationRequired");
        }
        PolicyUpdateAuditDetail auditDetail = new PolicyUpdateAuditDetail(
                List.of("INVOCATION_RETENTION: affects new invocations only"),
                List.of(),
                List.of(),
                List.of("Retention changes apply to new invocations only"),
                request.confirmed(),
                false,
                null
        );
        return executor.saveSingleDomain(
                templateId,
                session,
                List.of("INVOCATION_RETENTION"),
                existing -> existing.updateRetentionDomain(
                        request.saveGeneratedDocuments(),
                        request.invocationRecordRetentionDays(),
                        request.documentRetentionDays(),
                        session.username()
                ),
                auditDetail
        );
    }

    ApiPolicyView saveDefaultRouteDomain(
            UUID templateId,
            SaveDefaultRouteRequest request,
            ManagementSessionClaims session
    ) {
        access.requirePublishedTemplate(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        executor.assertDefaultRouteTargetCallable(templateId, request.defaultRouteReleaseVersion());
        String currentTarget = policy.getDefaultRouteReleaseVersion();
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withDefaultRoute(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of("DEFAULT_ROUTE_TARGET: " + currentTarget + " -> " + request.defaultRouteReleaseVersion())
        );
        return executor.saveSingleDomain(
                templateId,
                session,
                List.of("DEFAULT_ROUTE_TARGET"),
                existing -> existing.updateDefaultRouteDomain(request.defaultRouteReleaseVersion(), session.username()),
                auditDetail
        );
    }
}
