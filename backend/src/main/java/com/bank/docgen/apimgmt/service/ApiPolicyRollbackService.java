package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.RollbackApiPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiPolicyRollbackService {

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiPolicyVersionRepository apiPolicyVersionRepository;
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache;
    private final GroupAccessService groupAccessService;
    private final ObjectMapper objectMapper;
    private final ApiPolicyViewMapper apiPolicyViewMapper;

    public ApiPolicyRollbackService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiPolicyVersionRepository apiPolicyVersionRepository,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService,
            ManagementAuditRecorder managementAuditRecorder,
            TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache,
            GroupAccessService groupAccessService,
            ObjectMapper objectMapper,
            ApiPolicyViewMapper apiPolicyViewMapper
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiPolicyVersionRepository = apiPolicyVersionRepository;
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.apiPolicyVersionSnapshotService = apiPolicyVersionSnapshotService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.templateAdGroupAuthorizationCache = templateAdGroupAuthorizationCache;
        this.groupAccessService = groupAccessService;
        this.objectMapper = objectMapper;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
    }

    @Transactional(readOnly = true)
    public ApiPolicyImpactPreviewView previewRollback(
            UUID templateId,
            int targetPolicyVersion,
            ManagementSessionClaims session
    ) {
        requirePublishedTemplate(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        UpsertApiPolicyRequest candidate = loadCandidateRequest(templateId, targetPolicyVersion);
        return apiPolicyImpactPreviewService.preview(templateId, candidate, session);
    }

    @Transactional
    public ApiPolicyView rollback(
            UUID templateId,
            RollbackApiPolicyRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requirePublishedTemplate(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        int previousVersion = policy.getPolicyVersion();
        int sourceVersion = request.policyVersion();
        UpsertApiPolicyRequest candidate = loadCandidateRequest(templateId, sourceVersion);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);

        if (preview.blocking()) {
            throw new TemplateValidationException("api.error.apimgmt.policyImpactBlocked");
        }
        if (!preview.warnings().isEmpty() && !request.confirmed()) {
            throw new TemplateValidationException("api.error.apimgmt.policyImpactConfirmationRequired");
        }

        JsonNode snapshot = loadSnapshotNode(templateId, sourceVersion);
        String previousAdGroups = policy.getAllowedAdGroupsJson();
        policy.applyRollbackConfiguration(
                writeJson(readStringList(snapshot, "allowedAdGroups")),
                textOrNull(snapshot, "defaultRouteReleaseVersion"),
                writeJson(readStringList(snapshot, "outputFormats")),
                writeJson(readStringList(snapshot, "outputModes")),
                snapshot.path("batchEnabled").asBoolean(false),
                batchSyncMaxItems(snapshot),
                batchAsyncMaxItems(snapshot),
                snapshot.path("docxEncryptionEnabled").asBoolean(false),
                snapshot.path("pdfEncryptionEnabled").asBoolean(false),
                session.username()
        );
        apiPolicyRepository.save(policy);
        List<String> changedAreas = preview.changedAreas();
        apiPolicyVersionSnapshotService.snapshot(policy, changedAreas);

        if (!Objects.equals(previousAdGroups, policy.getAllowedAdGroupsJson())) {
            templateAdGroupAuthorizationCache.invalidate(templateId);
        }

        PolicyUpdateAuditDetail auditDetail = new PolicyUpdateAuditDetail(
                buildConfigDiffSummary(previousVersion, sourceVersion, preview),
                buildImpactPreviewSummary(preview),
                List.of(),
                preview.warnings(),
                request.confirmed() || preview.warnings().isEmpty(),
                true,
                sourceVersion
        );
        managementAuditRecorder.recordPolicyUpdated(
                templateId,
                template.getGroupCode(),
                previousVersion,
                policy.getPolicyVersion(),
                changedAreas,
                session.username(),
                actorSummary(session),
                auditDetail
        );
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    private UpsertApiPolicyRequest loadCandidateRequest(UUID templateId, int targetPolicyVersion) {
        JsonNode snapshot = loadSnapshotNode(templateId, targetPolicyVersion);
        return new UpsertApiPolicyRequest(
                readStringList(snapshot, "allowedAdGroups"),
                textOrNull(snapshot, "defaultRouteReleaseVersion"),
                readStringList(snapshot, "outputFormats"),
                readStringList(snapshot, "outputModes"),
                snapshot.path("batchEnabled").asBoolean(false),
                batchSyncMaxItems(snapshot),
                snapshot.path("docxEncryptionEnabled").asBoolean(false),
                snapshot.path("pdfEncryptionEnabled").asBoolean(false)
        );
    }

    private JsonNode loadSnapshotNode(UUID templateId, int targetPolicyVersion) {
        ApiPolicyVersionEntity history = apiPolicyVersionRepository
                .findByTemplateIdAndPolicyVersion(templateId, targetPolicyVersion)
                .orElseThrow(() -> new TemplateValidationException("api.error.apimgmt.policyVersionNotFound"));
        try {
            return objectMapper.readTree(history.getConfigSnapshotJson());
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.apimgmt.policyVersionNotFound");
        }
    }

    private int batchSyncMaxItems(JsonNode snapshot) {
        if (snapshot.has("batchSyncMaxItems")) {
            return snapshot.path("batchSyncMaxItems").asInt(100);
        }
        return snapshot.path("maxBatchSize").asInt(100);
    }

    private int batchAsyncMaxItems(JsonNode snapshot) {
        if (snapshot.has("batchAsyncMaxItems")) {
            return snapshot.path("batchAsyncMaxItems").asInt(10000);
        }
        return snapshot.path("maxBatchSize").asInt(10000);
    }

    private List<String> buildConfigDiffSummary(
            int previousVersion,
            int sourceVersion,
            ApiPolicyImpactPreviewView preview
    ) {
        List<String> summary = new ArrayList<>();
        summary.add("ROLLBACK: v" + previousVersion + " -> snapshot v" + sourceVersion + " as v" + preview.nextPolicyVersion());
        if (preview.contractDiffSummary() != null && !preview.contractDiffSummary().isBlank()) {
            summary.add(preview.contractDiffSummary());
        }
        return summary;
    }

    private List<String> buildImpactPreviewSummary(ApiPolicyImpactPreviewView preview) {
        List<String> summary = new ArrayList<>();
        summary.add(preview.summaryMessageKey());
        if (preview.idempotencyImpactSummary() != null && !preview.idempotencyImpactSummary().isBlank()) {
            summary.add(preview.idempotencyImpactSummary());
        }
        return summary;
    }

    private TemplateEntity requirePublishedTemplate(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                && template.getLifecycleStatus() != TemplateLifecycleStatus.PENDING_RELEASE) {
            throw new TemplateValidationException("api.error.apimgmt.templateNotPublished");
        }
        return template;
    }

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    private String textOrNull(JsonNode snapshot, String field) {
        JsonNode node = snapshot.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private List<String> readStringList(JsonNode snapshot, String field) {
        JsonNode node = snapshot.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        }
        try {
            return objectMapper.convertValue(node, new TypeReference<List<String>>() {
            });
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
