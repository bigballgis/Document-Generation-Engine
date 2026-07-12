package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Package-private credential create / rotate / revoke / list commands.
 */
final class ApiCredentialCommandSupport {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final PasswordHashService passwordHashService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;

    ApiCredentialCommandSupport(
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder managementAuditRecorder,
            ApiPolicyViewMapper apiPolicyViewMapper,
            ApiManagementAccessSupport access
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.passwordHashService = passwordHashService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.access = access;
    }

    List<ApiCredentialSummaryView> listCredentials(UUID templateId, ManagementSessionClaims session) {
        access.requireApiAdmin(templateId, session);
        return apiCredentialRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .map(apiPolicyViewMapper::toCredentialSummary)
                .toList();
    }

    ApiCredentialCreatedView createCredential(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireApiAdmin(templateId, session);
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
                access.actorSummary(session)
        );
        return new ApiCredentialCreatedView(
                credential.getId().toString(),
                credential.getExternalId(),
                secret,
                credential.getStatus().name(),
                credential.getCreatedAt()
        );
    }

    RotateCredentialResponse rotateCredential(
            UUID templateId,
            UUID credentialId,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireApiAdmin(templateId, session);
        ApiCredentialEntity credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(ApiManagementNotFoundException::new);
        if (!credential.getTemplateId().equals(templateId)) {
            throw new ApiManagementNotFoundException();
        }
        if (credential.getStatus() != ApiCredentialStatus.ACTIVE) {
            throw new TemplateValidationException("api.error.apimgmt.credentialNotActive");
        }
        String previousFingerprint = fingerprint(credential.getExternalId());
        String secret = generateSecret();
        credential.rotateSecret(passwordHashService.hash(secret));
        apiCredentialRepository.save(credential);
        managementAuditRecorder.recordCredentialRotated(
                templateId,
                template.getGroupCode(),
                credential.getId(),
                credential.getExternalId(),
                session.username(),
                access.actorSummary(session),
                credential.getRotationGeneration(),
                previousFingerprint
        );
        return new RotateCredentialResponse(
                credential.getId().toString(),
                credential.getExternalId(),
                secret,
                Instant.now()
        );
    }

    ApiCredentialSummaryView revokeCredential(UUID templateId, UUID credentialId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireApiAdmin(templateId, session);
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
                access.actorSummary(session)
        );
        return apiPolicyViewMapper.toCredentialSummary(credential);
    }

    private String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
