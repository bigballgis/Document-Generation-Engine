package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
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

    public ApiManagementService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            GroupAccessService groupAccessService,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder managementAuditRecorder,
            ContractAssemblyService contractAssemblyService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.groupAccessService = groupAccessService;
        this.passwordHashService = passwordHashService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.contractAssemblyService = contractAssemblyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ApiPolicyView getPolicy(UUID templateId, ManagementSessionClaims session) {
        requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        return toPolicyView(policy);
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
                .map(this::toRuntimeCredentialSummary)
                .orElse(null);
        return contractAssemblyService.assemble(template, policy, environment, credentialSummary);
    }

    @Transactional
    public ApiPolicyView upsertPolicy(UUID templateId, UpsertApiPolicyRequest request, ManagementSessionClaims session) {
        TemplateEntity template = requireApiAdmin(templateId, session);
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            throw new TemplateValidationException("api.error.apimgmt.templateNotPublished");
        }
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
        managementAuditRecorder.recordPolicyUpdated(
                templateId,
                template.getGroupCode(),
                previousVersion,
                policy.getPolicyVersion(),
                changedAreas,
                session.username(),
                actorSummary(session)
        );
        return toPolicyView(policy);
    }

    @Transactional(readOnly = true)
    public List<ApiCredentialSummaryView> listCredentials(UUID templateId, ManagementSessionClaims session) {
        requireApiAdmin(templateId, session);
        return apiCredentialRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .map(this::toCredentialSummary)
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
        return toCredentialSummary(credential);
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

    private ApiPolicyView toPolicyView(ApiPolicyEntity policy) {
        return new ApiPolicyView(
                policy.getTemplateId().toString(),
                policy.getPolicyVersion(),
                readStringList(policy.getAllowedAdGroupsJson()),
                policy.getDefaultRouteReleaseVersion(),
                readStringList(policy.getOutputFormatsJson()),
                readStringList(policy.getOutputModesJson()),
                policy.isBatchEnabled(),
                policy.getMaxBatchSize(),
                policy.isDocxEncryptionEnabled(),
                policy.isPdfEncryptionEnabled(),
                policy.getUpdatedAt()
        );
    }

    private ApiCredentialSummaryView toCredentialSummary(ApiCredentialEntity credential) {
        return new ApiCredentialSummaryView(
                credential.getId().toString(),
                credential.getExternalId(),
                credential.getStatus().name(),
                credential.getCreatedAt(),
                credential.getRevokedAt()
        );
    }

    private RuntimeCredentialSummaryView toRuntimeCredentialSummary(ApiCredentialEntity credential) {
        return new RuntimeCredentialSummaryView(
                credential.getExternalId(),
                credential.getStatus().name(),
                "fp-" + credential.getExternalId()
        );
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

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
