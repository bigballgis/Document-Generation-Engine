package com.bank.docgen.apimgmt.service;

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
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private API policy upsert and single-domain save commands (including AD groups).
 */
final class ApiPolicyCommandSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;
    private final ApiPolicyDomainSaveSupport domainSaves;

    ApiPolicyCommandSupport(
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
        this.apiPolicyVersionSnapshotService = apiPolicyVersionSnapshotService;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.access = access;
        this.domainSaves = new ApiPolicyDomainSaveSupport(
                apiPolicyRepository,
                managementAuditRecorder,
                objectMapper,
                apiPolicyVersionSnapshotService,
                templateVersionRepository,
                templateAdGroupAuthorizationCache,
                apiPolicyImpactPreviewService,
                apiPolicyViewMapper,
                access
        );
    }

    ApiPolicyView upsertPolicy(UUID templateId, UpsertApiPolicyRequest request, ManagementSessionClaims session) {
        TemplateEntity template = access.requirePublishedTemplate(templateId, session);
        String allowedJson = domainSaves.writeJson(request.allowedAdGroups());
        String outputFormatsJson = domainSaves.writeJson(request.outputFormats());
        String outputModesJson = domainSaves.writeJson(request.outputModes());
        Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);
        int previousVersion = existing.map(ApiPolicyEntity::getPolicyVersion).orElse(0);
        ApiPolicyEntity policy;
        List<String> changedAreas;
        if (existing.isPresent()) {
            policy = existing.get();
            changedAreas = ApiPolicyChangeAreaResolver.detectChangedAreas(
                    policy, request, allowedJson, outputFormatsJson, outputModesJson);
            policy.update(
                    allowedJson,
                    request.defaultRouteReleaseVersion(),
                    outputFormatsJson,
                    outputModesJson,
                    request.batchEnabled(),
                    request.maxBatchSize(),
                    request.docxEncryptionEnabled(),
                    request.pdfEncryptionEnabled(),
                    session.username()
            );
        } else {
            policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, allowedJson, session.username());
            policy.replaceConfiguration(
                    allowedJson,
                    request.defaultRouteReleaseVersion(),
                    outputFormatsJson,
                    outputModesJson,
                    request.batchEnabled(),
                    request.maxBatchSize(),
                    request.docxEncryptionEnabled(),
                    request.pdfEncryptionEnabled(),
                    session.username()
            );
            changedAreas = ApiPolicyChangeAreaResolver.initialChangedAreas();
        }
        apiPolicyRepository.save(policy);
        apiPolicyVersionSnapshotService.snapshot(policy, changedAreas);
        managementAuditRecorder.recordPolicyUpdated(
                templateId,
                template.getGroupCode(),
                previousVersion,
                policy.getPolicyVersion(),
                changedAreas,
                session.username(),
                access.actorSummary(session)
        );
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    ApiPolicyView saveAdGroupsDomain(UUID templateId, SaveAdGroupsRequest request, ManagementSessionClaims session) {
        return domainSaves.saveAdGroupsDomain(templateId, request, session);
    }

    ApiPolicyView saveOutputDomain(UUID templateId, SaveOutputPolicyRequest request, ManagementSessionClaims session) {
        return domainSaves.saveOutputDomain(templateId, request, session);
    }

    ApiPolicyView saveBatchLimitsDomain(
            UUID templateId, SaveBatchLimitsRequest request, ManagementSessionClaims session) {
        return domainSaves.saveBatchLimitsDomain(templateId, request, session);
    }

    ApiPolicyView saveEncryptionDomain(
            UUID templateId, SaveEncryptionPolicyRequest request, ManagementSessionClaims session) {
        return domainSaves.saveEncryptionDomain(templateId, request, session);
    }

    ApiPolicyView saveInvocationRetentionDomain(
            UUID templateId, SaveInvocationRetentionRequest request, ManagementSessionClaims session) {
        return domainSaves.saveInvocationRetentionDomain(templateId, request, session);
    }

    ApiPolicyView saveDefaultRouteDomain(
            UUID templateId, SaveDefaultRouteRequest request, ManagementSessionClaims session) {
        return domainSaves.saveDefaultRouteDomain(templateId, request, session);
    }
}
