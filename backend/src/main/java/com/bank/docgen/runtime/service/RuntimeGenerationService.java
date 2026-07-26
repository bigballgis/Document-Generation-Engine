package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.runtime.api.CallableVersionsResultView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.service.TemplateCallabilitySupport;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeGenerationService {

    private final TemplateVersionRepository templateVersionRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final ObjectStoragePort objectStoragePort;
    private final IdempotencyService idempotencyService;
    private final EncryptionParameterValidator encryptionParameterValidator;
    private final ContractAssemblyService contractAssemblyService;
    private final DocumentGenerationEngine documentGenerationEngine;
    private final VersionFidelityWarningService versionFidelityWarningService;
    private final RuntimeGenerationIdempotencySupport idempotencySupport;
    private final RuntimeGenerateRequestSupport requestSupport;

    public RuntimeGenerationService(
            TemplateVersionRepository templateVersionRepository,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            ObjectStoragePort objectStoragePort,
            IdempotencyService idempotencyService,
            EncryptionParameterValidator encryptionParameterValidator,
            ContractAssemblyService contractAssemblyService,
            DocumentGenerationEngine documentGenerationEngine,
            ObjectMapper objectMapper,
            VersionFidelityWarningService versionFidelityWarningService
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.objectStoragePort = objectStoragePort;
        this.idempotencyService = idempotencyService;
        this.encryptionParameterValidator = encryptionParameterValidator;
        this.contractAssemblyService = contractAssemblyService;
        this.documentGenerationEngine = documentGenerationEngine;
        this.versionFidelityWarningService = versionFidelityWarningService;
        this.idempotencySupport = new RuntimeGenerationIdempotencySupport(
                idempotencyService,
                templateVersionRepository
        );
        this.requestSupport = new RuntimeGenerateRequestSupport(objectMapper);
    }

    @Transactional(readOnly = true)
    public ContractResultView getContract(TemplateEntity template, RuntimeSessionClaims session, String environment) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new TemplateValidationException("api.error.runtime.policyNotConfigured"));
        ApiCredentialEntity credential = apiCredentialRepository
                .findByExternalId(session.credentialExternalId())
                .orElse(null);
        Instant now = Instant.now();
        RuntimeCredentialSummaryView credentialSummary = credential == null ? null
                : new RuntimeCredentialSummaryView(
                        credential.getExternalId(),
                        ApiCredentialLifecycleSupport.resolveEffectiveStatus(credential, now).name(),
                        "fp-" + credential.getExternalId(),
                        credential.getExpiresAt(),
                        ApiCredentialLifecycleSupport.isPreviousSecretWithinGrace(credential, now)
                                ? credential.getRotationGracePeriodEndsAt()
                                : null
                );
        return contractAssemblyService.assemble(
                template,
                policy,
                environment,
                credentialSummary,
                ContractViewAudience.CALLER
        );
    }

    @Transactional(readOnly = true)
    public CallableVersionsResultView listCallableVersionsResult(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment
    ) {
        assertTemplateAccess(template, session);
        return new CallableVersionsResultView(
                template.getExternalId(),
                contractAssemblyService.listCallableVersions(template, environment)
        );
    }

    @Transactional
    public SyncGenerateResult generateSync(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String releaseVersion,
            GenerateRequestBody request
    ) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new TemplateValidationException("api.error.runtime.policyNotConfigured"));
        requestSupport.validateGenerateRequest(request, policy);
        encryptionParameterValidator.validate(request.encryption(), policy, request.output().format());
        TemplateLocaleCompatibilitySupport.assertRequestLocaleCompatible(
                template,
                request.context() == null ? null : request.context().locale()
        );
        String resolvedVersion = releaseVersion != null ? releaseVersion : policy.getDefaultRouteReleaseVersion();
        if (resolvedVersion == null) {
            throw new TemplateValidationException("api.error.runtime.releaseVersionRequired");
        }
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), resolvedVersion)
                .orElseThrow(TemplateNotFoundException::new);
        TemplateCallabilitySupport.assertReleaseVersionCallable(template, version, resolvedVersion);
        String requestHash = idempotencyService.hashRequest(requestSupport.writeRequest(request, resolvedVersion));
        Optional<GenerationIdempotencyEntity> existing = idempotencySupport.findExistingIdempotency(
                request,
                template,
                releaseVersion,
                resolvedVersion,
                requestHash,
                requestSupport::writeRequest
        );
        if (existing.isPresent()) {
            GenerationIdempotencyEntity existingRecord = existing.get();
            if (existingRecord.getResponseStorageKey() != null) {
                InputStream replayStream = objectStoragePort.get(existingRecord.getResponseStorageKey());
                return new SyncGenerateResult(
                        null,
                        replayStream,
                        requestSupport.contentTypeForFormat(request.output().format()),
                        existingRecord.getDocumentId(),
                        resolvedVersion,
                        versionFidelityWarningService.resolveWarningCodes(
                                version,
                                template.getMasterId()
                        ),
                        IdempotencyConstants.STATUS_REPLAYED
                );
            }
            if ("IN_PROGRESS".equals(existingRecord.getStatus())) {
                throw IdempotencyConflictException.requestInProgress(request.idempotencyKey());
            }
        }
        GenerationIdempotencyEntity idempotency = existing.orElseGet(() ->
                idempotencyService.begin(request.idempotencyKey(), template.getId(), requestHash, resolvedVersion));
        DocumentGenerationEngine.GeneratedDocument generated = documentGenerationEngine.generate(
                template,
                resolvedVersion,
                request.variables(),
                request.output().format(),
                request.encryption(),
                com.bank.docgen.authoring.structured.CallerRenderOverride.empty(),
                "sync",
                request.context() == null ? null : request.context().locale(),
                inclusionAxesFrom(request),
                request.context() == null ? null : request.context().legalEntityCode()
        );
        idempotencyService.complete(idempotency, generated.storageKey(), generated.documentId());
        InputStream artifactStream = objectStoragePort.get(generated.storageKey());
        return new SyncGenerateResult(
                null,
                artifactStream,
                generated.contentType(),
                generated.documentId(),
                resolvedVersion,
                generated.fidelityWarningCodes(),
                IdempotencyConstants.STATUS_NEW,
                generated.resolvedLegalEntityCode(),
                generated.resolvedDocumentBrandCode()
        );
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }

    private static CompositionInclusionAxes inclusionAxesFrom(GenerateRequestBody request) {
        if (request.context() == null) {
            return CompositionInclusionAxes.empty();
        }
        return CompositionInclusionAxes.of(
                request.context().jurisdiction(),
                request.context().product(),
                request.context().channel()
        );
    }
}
