package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiManagementService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final GroupAccessService groupAccessService;
    private final PasswordHashService passwordHashService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ContractAssemblyService contractAssemblyService;
    private final ObjectMapper objectMapper;
    private final ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache;
    private final ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;
    private final ApiPolicyViewMapper apiPolicyViewMapper;

    public ApiManagementService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            GroupAccessService groupAccessService,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder managementAuditRecorder,
            ContractAssemblyService contractAssemblyService,
            ObjectMapper objectMapper,
            ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService,
            TemplateVersionRepository templateVersionRepository,
            TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiPolicyViewMapper apiPolicyViewMapper
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.groupAccessService = groupAccessService;
        this.passwordHashService = passwordHashService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.contractAssemblyService = contractAssemblyService;
        this.objectMapper = objectMapper;
        this.apiPolicyVersionSnapshotService = apiPolicyVersionSnapshotService;
        this.templateVersionRepository = templateVersionRepository;
        this.templateAdGroupAuthorizationCache = templateAdGroupAuthorizationCache;
        this.apiPolicyImpactPreviewService = apiPolicyImpactPreviewService;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
    }

    @Transactional(readOnly = true)
    public ApiPolicyView getPolicy(UUID templateId, ManagementSessionClaims session) {
        requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    @Transactional(readOnly = true)
    public ContractResultView getCallerContract(
            UUID templateId,
            String environment,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        RuntimeCredentialSummaryView credentialSummary = apiCredentialRepository
                .findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .filter(credential -> credential.getStatus() == ApiCredentialStatus.ACTIVE)
                .findFirst()
                .map(apiPolicyViewMapper::toRuntimeCredentialSummary)
                .orElse(null);
        return contractAssemblyService.assemble(
                template,
                policy,
                environment,
                credentialSummary,
                ContractViewAudience.ADMIN
        );
    }

    @Transactional
    public ApiPolicyView upsertPolicy(UUID templateId, UpsertApiPolicyRequest request, ManagementSessionClaims session) {
        TemplateEntity template = requirePublishedTemplate(templateId, session);
        String allowedJson = writeJson(request.allowedAdGroups());
        String outputFormatsJson = writeJson(request.outputFormats());
        String outputModesJson = writeJson(request.outputModes());
        Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);
        int previousVersion = existing.map(ApiPolicyEntity::getPolicyVersion).orElse(0);
        ApiPolicyEntity policy;
        List<String> changedAreas;
        if (existing.isPresent()) {
            policy = existing.get();
            changedAreas = ApiPolicyChangeAreaResolver.detectChangedAreas(
                    policy,
                    request,
                    allowedJson,
                    outputFormatsJson,
                    outputModesJson
            );
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
                actorSummary(session)
        );
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    @Transactional
    public ApiPolicyView saveAdGroupsDomain(UUID templateId, SaveAdGroupsRequest request, ManagementSessionClaims session) {
        ApiPolicyEntity policy = requirePolicyHead(templateId, session);
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

    @Transactional
    public ApiPolicyView saveOutputDomain(UUID templateId, SaveOutputPolicyRequest request, ManagementSessionClaims session) {
        ApiPolicyEntity policy = requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withOutput(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of()
        );
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

    @Transactional
    public ApiPolicyView saveBatchLimitsDomain(
            UUID templateId,
            SaveBatchLimitsRequest request,
            ManagementSessionClaims session
    ) {
        ApiPolicyEntity policy = requirePolicyHead(templateId, session);
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

    @Transactional
    public ApiPolicyView saveEncryptionDomain(
            UUID templateId,
            SaveEncryptionPolicyRequest request,
            ManagementSessionClaims session
    ) {
        ApiPolicyEntity policy = requirePolicyHead(templateId, session);
        UpsertApiPolicyRequest candidate = ApiPolicyCandidateBuilder.withEncryption(policy, request, objectMapper);
        ApiPolicyImpactPreviewView preview = apiPolicyImpactPreviewService.preview(templateId, candidate, session);
        ApiPolicySaveGate.requireSaveAllowed(preview, request.confirmed());
        PolicyUpdateAuditDetail auditDetail = ApiPolicySaveGate.auditDetailFromPreview(
                preview,
                request.confirmed(),
                List.of()
        );
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

    @Transactional
    public ApiPolicyView saveDefaultRouteDomain(
            UUID templateId,
            SaveDefaultRouteRequest request,
            ManagementSessionClaims session
    ) {
        requirePublishedTemplate(templateId, session);
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

    private ApiPolicyEntity requirePolicyHead(UUID templateId, ManagementSessionClaims session) {
        requirePublishedTemplate(templateId, session);
        return apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
    }

    private ApiPolicyView saveSingleDomain(
            UUID templateId,
            ManagementSessionClaims session,
            List<String> changedAreas,
            java.util.function.Consumer<ApiPolicyEntity> domainUpdater,
            PolicyUpdateAuditDetail auditDetail
    ) {
        TemplateEntity template = requirePublishedTemplate(templateId, session);
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
                actorSummary(session),
                auditDetail
        );
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    private TemplateEntity requirePublishedTemplate(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                && template.getLifecycleStatus() != TemplateLifecycleStatus.PENDING_RELEASE) {
            throw new TemplateValidationException("api.error.apimgmt.templateNotPublished");
        }
        return template;
    }

    @Transactional(readOnly = true)
    public List<ApiCredentialSummaryView> listCredentials(UUID templateId, ManagementSessionClaims session) {
        requireApiAdmin(templateId, session);
        return apiCredentialRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .map(apiPolicyViewMapper::toCredentialSummary)
                .toList();
    }

    @Transactional
    public ApiCredentialCreatedView createCredential(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        apiPolicyRepository.findByTemplateId(templateId).orElseThrow(ApiManagementNotFoundException::new);
        String externalId = "CRED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String secret = generateSecret();
        ApiCredentialEntity credential = new ApiCredentialEntity(
                UUID.randomUUID(),
                externalId,
                templateId,
                passwordHashService.hash(secret),
                session.username()
        );
        apiCredentialRepository.save(credential);
        managementAuditRecorder.recordCredentialCreated(
                templateId,
                template.getGroupCode(),
                credential.getId(),
                credential.getExternalId(),
                session.username(),
                actorSummary(session)
        );
        return new ApiCredentialCreatedView(
                credential.getId().toString(),
                credential.getExternalId(),
                secret,
                credential.getStatus().name(),
                credential.getCreatedAt()
        );
    }

    @Transactional
    public RotateCredentialResponse rotateCredential(
            UUID templateId,
            UUID credentialId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        ApiCredentialEntity credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(ApiManagementNotFoundException::new);
        if (!credential.getTemplateId().equals(templateId)) {
            throw new ApiManagementNotFoundException();
        }
        if (credential.getStatus() != ApiCredentialStatus.ACTIVE) {
            throw new TemplateValidationException("api.error.apimgmt.credentialNotActive");
        }
        String secret = generateSecret();
        credential.rotateSecret(passwordHashService.hash(secret));
        apiCredentialRepository.save(credential);
        managementAuditRecorder.recordCredentialRotated(
                templateId,
                template.getGroupCode(),
                credential.getId(),
                credential.getExternalId(),
                session.username(),
                actorSummary(session)
        );
        return new RotateCredentialResponse(
                credential.getId().toString(),
                credential.getExternalId(),
                secret,
                Instant.now()
        );
    }

    @Transactional
    public ApiCredentialSummaryView revokeCredential(UUID templateId, UUID credentialId, ManagementSessionClaims session) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        ApiCredentialEntity credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(ApiManagementNotFoundException::new);
        if (!credential.getTemplateId().equals(templateId)) {
            throw new ApiManagementNotFoundException();
        }
        credential.revoke();
        apiCredentialRepository.save(credential);
        managementAuditRecorder.recordCredentialRevoked(
                templateId,
                template.getGroupCode(),
                credential.getId(),
                credential.getExternalId(),
                session.username(),
                actorSummary(session)
        );
        return apiPolicyViewMapper.toCredentialSummary(credential);
    }

    private TemplateEntity requireApiAdmin(UUID templateId, ManagementSessionClaims session) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
        return templateService.requireReadableTemplate(templateId, session);
    }

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
