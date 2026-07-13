package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;

/**
 * Package-private single-domain API policy saves and shared persistence helpers.
 */
final class ApiPolicyDomainSaveSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ObjectMapper objectMapper;
    private final ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache;
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;

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
        this.apiPolicyRepository = apiPolicyRepository;
        this.managementAuditRecorder = managementAuditRecorder;
        this.objectMapper = objectMapper;
        this.apiPolicyVersionSnapshotService = apiPolicyVersionSnapshotService;
        this.templateVersionRepository = templateVersionRepository;
        this.templateAdGroupAuthorizationCache = templateAdGroupAuthorizationCache;
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.access = access;
    }

    String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    ApiPolicyView saveAdGroupsDomain(UUID templateId, SaveAdGroupsRequest request, ManagementSessionClaims session) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withAdGroups(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of("AD_GROUP_AUTHORIZATION: groupCount=" + request.allowedAdGroups().size())
        );
        ApiPolicyView view = saveSingleDomain(
                templateId,
                session,
                List.of("AD_GROUP_AUTHORIZATION"),
                existing -> existing.updateAdGroupsDomain(writeJson(request.allowedAdGroups()), session.username()),
                auditDetail
        );
        templateAdGroupAuthorizationCache.invalidate(templateId);
        return view;
    }

    ApiPolicyView saveOutputDomain(UUID templateId, SaveOutputPolicyRequest request, ManagementSessionClaims session) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withOutput(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview, request.confirmed(), List.of());
        return saveSingleDomain(
                templateId,
                session,
                List.of("OUTPUT_POLICY"),
                existing -> existing.updateOutputDomain(
                        writeJson(request.outputFormats()),
                        writeJson(request.outputModes()),
                        session.username()
                ),
                auditDetail
        );
    }

    ApiPolicyView saveBatchLimitsDomain(
            UUID templateId,
            SaveBatchLimitsRequest request,
            ManagementSessionClaims session
    ) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withBatchLimits(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of("BATCH_LIMIT: syncMax=" + request.syncMaxItems() + ", asyncMax=" + request.asyncMaxItems())
        );
        return saveSingleDomain(
                templateId,
                session,
                List.of("BATCH_LIMIT"),
                existing -> existing.updateBatchLimitsDomain(
                        request.batchEnabled(),
                        request.syncMaxItems(),
                        request.asyncMaxItems(),
                        session.username()
                ),
                auditDetail
        );
    }

    ApiPolicyView saveEncryptionDomain(
            UUID templateId,
            SaveEncryptionPolicyRequest request,
            ManagementSessionClaims session
    ) {
        ApiPolicyEntity policy = access.requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withEncryption(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview, request.confirmed(), List.of());
        return saveSingleDomain(
                templateId,
                session,
                List.of("ENCRYPTION_CAPABILITY"),
                existing -> existing.updateEncryptionDomain(
                        request.docxEncryptionEnabled(),
                        request.pdfEncryptionEnabled(),
                        session.username()
                ),
                auditDetail
        );
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
        if (retentionChanged(policy, request) && !request.confirmed()) {
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
        return saveSingleDomain(
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
        assertDefaultRouteTargetCallable(templateId, request.defaultRouteReleaseVersion());
        String currentTarget = policy.getDefaultRouteReleaseVersion();
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withDefaultRoute(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of("DEFAULT_ROUTE_TARGET: " + currentTarget + " -> " + request.defaultRouteReleaseVersion())
        );
        return saveSingleDomain(
                templateId,
                session,
                List.of("DEFAULT_ROUTE_TARGET"),
                existing -> existing.updateDefaultRouteDomain(request.defaultRouteReleaseVersion(), session.username()),
                auditDetail
        );
    }

    private void assertDefaultRouteTargetCallable(UUID templateId, String targetVersion) {
        if (targetVersion == null || targetVersion.isBlank()) {
            throw new TemplateValidationException("api.error.apimgmt.defaultRouteTargetNotCallable");
        }
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, targetVersion)
                .orElseThrow(() -> new TemplateValidationException("api.error.apimgmt.defaultRouteTargetNotCallable"));
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            throw new TemplateValidationException("api.error.apimgmt.defaultRouteTargetNotCallable");
        }
    }

    private static boolean retentionChanged(ApiPolicyEntity policy, SaveInvocationRetentionRequest request) {
        return policy.isSaveGeneratedDocuments() != request.saveGeneratedDocuments()
                || policy.getInvocationRecordRetentionDays() != request.invocationRecordRetentionDays()
                || policy.getDocumentRetentionDays() != request.documentRetentionDays();
    }

    private ApiPolicyView saveSingleDomain(
            UUID templateId,
            ManagementSessionClaims session,
            List<String> changedAreas,
            java.util.function.Consumer<ApiPolicyEntity> domainUpdater,
            PolicyUpdateAuditDetail auditDetail
    ) {
        TemplateEntity template = access.requirePublishedTemplate(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        int previousVersion = policy.getPolicyVersion();
        domainUpdater.accept(policy);
        apiPolicyRepository.save(policy);
        apiPolicyVersionSnapshotService.snapshot(policy, changedAreas);
        managementAuditRecorder.recordPolicyUpdated(
                templateId,
                template.getGroupCode(),
                previousVersion,
                policy.getPolicyVersion(),
                changedAreas,
                session.username(),
                access.actorSummary(session),
                auditDetail
        );
        return apiPolicyViewMapper.toPolicyView(policy);
    }
}
