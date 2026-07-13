package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Package-private preview / confirm / save orchestration for single-domain policy updates.
 */
final class ApiPolicyDomainPreviewSaveSupport {

    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiManagementAccessSupport access;
    private final ApiPolicyDomainSaveExecutorSupport executor;

    ApiPolicyDomainPreviewSaveSupport(
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiManagementAccessSupport access,
            ApiPolicyDomainSaveExecutorSupport executor
    ) {
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.access = access;
        this.executor = executor;
    }

    ApiPolicyView saveWithPreview(
            UUID templateId,
            ManagementSessionClaims session,
            boolean confirmed,
            Function<ApiPolicyEntity, UpsertApiPolicyRequest> candidateFactory,
            List<String> changeAreas,
            List<String> extraAuditNotes,
            Consumer<ApiPolicyEntity> mutator
    ) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = candidateFactory.apply(policy);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, confirmed);
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview, confirmed, extraAuditNotes);
        return executor.saveSingleDomain(templateId, session, changeAreas, mutator, auditDetail);
    }
}
