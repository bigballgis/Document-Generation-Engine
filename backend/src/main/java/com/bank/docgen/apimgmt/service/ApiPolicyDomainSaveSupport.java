package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

/**
 * Package-private single-domain API policy saves and shared persistence helpers.
 */
final class ApiPolicyDomainSaveSupport {

    private final ObjectMapper objectMapper;
    private final TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache;
    private final ApiPolicyDomainSaveExecutorSupport executor;
    private final ApiPolicyDomainPreviewSaveSupport previewSave;
    private final ApiPolicyDomainSpecialSaveSupport specialSave;

    ApiPolicyDomainSaveSupport(
            ApiPolicyRepository apiPolicyRepository,
            ManagementAuditRecorder managementAuditRecorder,
            ObjectMapper objectMapper,
            ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService,
            TemplateVersionRepository templateVersionRepository,
            TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiPolicyViewMapper apiPolicyViewMapper,
            ApiManagementAccessSupport access
    ) {
        this.objectMapper = objectMapper;
        this.templateAdGroupAuthorizationCache = templateAdGroupAuthorizationCache;
        this.executor = new ApiPolicyDomainSaveExecutorSupport(
                apiPolicyRepository,
                managementAuditRecorder,
                objectMapper,
                apiPolicyVersionSnapshotService,
                templateVersionRepository,
                apiPolicyViewMapper,
                access
        );
        this.previewSave = new ApiPolicyDomainPreviewSaveSupport(
                apiPolicyImpactPreviewService, access, executor);
        this.specialSave = new ApiPolicyDomainSpecialSaveSupport(
                apiPolicyRepository, objectMapper, apiPolicyImpactPreviewService, access, executor);
    }

    String writeJson(List<String> values) {
        return executor.writeJson(values);
    }

    ApiPolicyView saveAdGroupsDomain(UUID templateId, SaveAdGroupsRequest request, ManagementSessionClaims session) {
        ApiPolicyView view = previewSave.saveWithPreview(
                templateId, session, request.confirmed(),
                policy -> ApiPolicyCandidateBuilder.withAdGroups(policy, request, objectMapper),
                List.of("AD_GROUP_AUTHORIZATION"),
                List.of("AD_GROUP_AUTHORIZATION: groupCount=" + request.allowedAdGroups().size()),
                existing -> existing.updateAdGroupsDomain(executor.writeJson(request.allowedAdGroups()), session.username())
        );
        templateAdGroupAuthorizationCache.invalidate(templateId);
        return view;
    }

    ApiPolicyView saveOutputDomain(UUID templateId, SaveOutputPolicyRequest request, ManagementSessionClaims session) {
        return previewSave.saveWithPreview(
                templateId, session, request.confirmed(),
                policy -> ApiPolicyCandidateBuilder.withOutput(policy, request, objectMapper),
                List.of("OUTPUT_POLICY"), List.of(),
                existing -> existing.updateOutputDomain(
                        executor.writeJson(request.outputFormats()),
                        executor.writeJson(request.outputModes()),
                        session.username())
        );
    }

    ApiPolicyView saveBatchLimitsDomain(
            UUID templateId, SaveBatchLimitsRequest request, ManagementSessionClaims session) {
        return previewSave.saveWithPreview(
                templateId, session, request.confirmed(),
                policy -> ApiPolicyCandidateBuilder.withBatchLimits(policy, request, objectMapper),
                List.of("BATCH_LIMIT"),
                List.of("BATCH_LIMIT: syncMax=" + request.syncMaxItems() + ", asyncMax=" + request.asyncMaxItems()),
                existing -> existing.updateBatchLimitsDomain(
                        request.batchEnabled(), request.syncMaxItems(), request.asyncMaxItems(), session.username())
        );
    }

    ApiPolicyView saveEncryptionDomain(
            UUID templateId, SaveEncryptionPolicyRequest request, ManagementSessionClaims session) {
        return previewSave.saveWithPreview(
                templateId, session, request.confirmed(),
                policy -> ApiPolicyCandidateBuilder.withEncryption(policy, request, objectMapper),
                List.of("ENCRYPTION_CAPABILITY"), List.of(),
                existing -> existing.updateEncryptionDomain(
                        request.docxEncryptionEnabled(), request.pdfEncryptionEnabled(), session.username())
        );
    }

    ApiPolicyView saveInvocationRetentionDomain(
            UUID templateId, SaveInvocationRetentionRequest request, ManagementSessionClaims session) {
        return specialSave.saveInvocationRetentionDomain(templateId, request, session);
    }

    ApiPolicyView saveDefaultRouteDomain(
            UUID templateId, SaveDefaultRouteRequest request, ManagementSessionClaims session) {
        return specialSave.saveDefaultRouteDomain(templateId, request, session);
    }
}
