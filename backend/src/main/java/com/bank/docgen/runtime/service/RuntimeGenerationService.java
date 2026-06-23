package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.runtime.api.CallableVersionsResultView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.EncryptionOptionsView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeGenerationService {

    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final IdempotencyService idempotencyService;
    private final EncryptionParameterValidator encryptionParameterValidator;
    private final ContractAssemblyService contractAssemblyService;
    private final ObjectMapper objectMapper;

    public RuntimeGenerationService(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            IdempotencyService idempotencyService,
            EncryptionParameterValidator encryptionParameterValidator,
            ContractAssemblyService contractAssemblyService,
            ObjectMapper objectMapper
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.idempotencyService = idempotencyService;
        this.encryptionParameterValidator = encryptionParameterValidator;
        this.contractAssemblyService = contractAssemblyService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ContractResultView getContract(TemplateEntity template, RuntimeSessionClaims session, String environment) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new TemplateValidationException("api.error.runtime.policyNotConfigured"));
        ApiCredentialEntity credential = apiCredentialRepository
                .findByExternalId(session.credentialExternalId())
                .orElse(null);
        RuntimeCredentialSummaryView credentialSummary = credential == null ? null
                : new RuntimeCredentialSummaryView(
                        credential.getExternalId(),
                        credential.getStatus().name(),
                        "fp-" + credential.getExternalId()
                );
        return contractAssemblyService.assemble(template, policy, environment, credentialSummary);
    }

    @Transactional(readOnly = true)
    public CallableVersionsResultView listCallableVersionsResult(TemplateEntity template, String environment) {
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
        validateGenerateRequest(request, policy);
        encryptionParameterValidator.validate(request.encryption(), policy, request.output().format());
        String resolvedVersion = releaseVersion != null ? releaseVersion : policy.getDefaultRouteReleaseVersion();
        if (resolvedVersion == null) {
            throw new TemplateValidationException("api.error.runtime.releaseVersionRequired");
        }
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                || !resolvedVersion.equals(template.getReleaseVersion())) {
            throw new TemplateValidationException("api.error.runtime.versionNotCallable");
        }
        String requestHash = idempotencyService.hashRequest(writeRequest(request, resolvedVersion));
        Optional<GenerationIdempotencyEntity> existing = idempotencyService.findExisting(
                request.idempotencyKey(),
                template.getId(),
                requestHash
        );
        if (existing.isPresent() && existing.get().getResponseStorageKey() != null) {
            byte[] cached;
            try (InputStream stream = objectStoragePort.get(existing.get().getResponseStorageKey())) {
                cached = stream.readAllBytes();
            } catch (java.io.IOException ex) {
                throw new TemplateValidationException("api.error.rendering.generationFailed");
            }
            return new SyncGenerateResult(
                    cached,
                    contentTypeForFormat(request.output().format()),
                    existing.get().getDocumentId(),
                    resolvedVersion,
                    List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name()),
                    "REPLAYED"
            );
        }
        GenerationIdempotencyEntity idempotency = existing.orElseGet(() ->
                idempotencyService.begin(request.idempotencyKey(), template.getId(), requestHash));
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), resolvedVersion)
                .orElseThrow(TemplateNotFoundException::new);
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(TemplateNotFoundException::new);
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> bindingJson = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        Map<String, String> anchorContent = docxAssembler.buildAnchorReplacements(bindingJson, request.variables());
        byte[] docx;
        try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
            docx = docxAssembler.assemble(masterStream, anchorContent);
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
        DocumentArtifactPipeline.GeneratedArtifact artifact = documentArtifactPipeline.finalizeArtifact(
                docx,
                request.output().format(),
                request.encryption()
        );
        String documentId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String storageKey = "generated/" + documentId + "/" + artifact.storageFileName();
        objectStoragePort.put(
                storageKey,
                new java.io.ByteArrayInputStream(artifact.bytes()),
                artifact.bytes().length,
                artifact.contentType()
        );
        idempotencyService.complete(idempotency, storageKey, documentId);
        return new SyncGenerateResult(
                artifact.bytes(),
                artifact.contentType(),
                documentId,
                resolvedVersion,
                List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name()),
                "CREATED"
        );
    }

    private String contentTypeForFormat(String outputFormat) {
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            return "application/pdf";
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }

    private void validateGenerateRequest(GenerateRequestBody request, ApiPolicyEntity policy) {
        if (request.output() == null || request.variables() == null) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
        String format = request.output().format();
        if (!"DOCX".equalsIgnoreCase(format) && !"PDF".equalsIgnoreCase(format)) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (readStringList(policy.getOutputFormatsJson()).stream().noneMatch(item -> item.equalsIgnoreCase(format))) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new TemplateValidationException("api.error.runtime.idempotencyKeyRequired");
        }
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String writeRequest(GenerateRequestBody request, String releaseVersion) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "releaseVersion", releaseVersion,
                    "variables", request.variables(),
                    "output", request.output()
            ));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return releaseVersion;
        }
    }
}
