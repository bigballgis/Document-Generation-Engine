package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
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
 * Package-private shared persistence helpers for single-domain API policy saves.
 */
final class ApiPolicyDomainSaveExecutorSupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ObjectMapper objectMapper;
    private final ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService;
    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;

    ApiPolicyDomainSaveExecutorSupport(
            ApiPolicyRepository apiPolicyRepository,
            ManagementAuditRecorder managementAuditRecorder,
            ObjectMapper objectMapper,
            ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService,
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyViewMapper apiPolicyViewMapper,
            ApiManagementAccessSupport access
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.managementAuditRecorder = managementAuditRecorder;
        this.objectMapper = objectMapper;
        this.apiPolicyVersionSnapshotService = apiPolicyVersionSnapshotService;
        this.templateVersionRepository = templateVersionRepository;
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

    void assertDefaultRouteTargetCallable(UUID templateId, String targetVersion) {
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

    static boolean retentionChanged(ApiPolicyEntity policy, SaveInvocationRetentionRequest request) {
        return policy.isSaveGeneratedDocuments() != request.saveGeneratedDocuments()
                || policy.getInvocationRecordRetentionDays() != request.invocationRecordRetentionDays()
                || policy.getDocumentRetentionDays() != request.documentRetentionDays();
    }

    ApiPolicyView saveSingleDomain(
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
