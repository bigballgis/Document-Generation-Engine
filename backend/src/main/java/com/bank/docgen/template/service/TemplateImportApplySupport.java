package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private bundle artifact / policy snapshot application for TemplateImportService.
 */
final class TemplateImportApplySupport {

    private final TemplateService templateService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ObjectMapper objectMapper;

    TemplateImportApplySupport(
            TemplateService templateService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            ApiPolicyRepository apiPolicyRepository,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.objectMapper = objectMapper;
    }

    void applyBundleArtifacts(
            UUID templateId,
            TemplateExportBundleView bundle,
            ManagementSessionClaims session
    ) {
        for (VariableSchemaView variable : bundle.variables()) {
            templateService.upsertVariable(
                    templateId,
                    new UpsertVariableSchemaRequest(
                            variable.variableKey(),
                            variable.variableType(),
                            variable.required(),
                            variable.defaultValue(),
                            variable.enumValues(),
                            variable.description(),
                            variable.computeExpression()
                    ),
                    session
            );
        }
        for (AnchorBindingView binding : bundle.bindings()) {
            templateService.upsertBinding(
                    templateId,
                    new UpsertAnchorBindingRequest(
                            binding.anchorId(),
                            AnchorContentType.valueOf(binding.declaredContentType()),
                            binding.structuredContentJson(),
                            binding.pasteCleaningEvidence(),
                            null
                    ),
                    session
            );
        }
        templateService.saveRules(templateId, bundle.rules(), session);
        for (ContentModuleReferenceView reference : bundle.contentModuleReferences()) {
            if (reference.locked()) {
                continue;
            }
            contentModuleReferenceService.upsertReference(
                    templateId,
                    new UpsertContentModuleReferenceRequest(
                            reference.referenceKey(),
                            reference.moduleId(),
                            reference.semanticVersion()
                    ),
                    session
            );
        }
    }

    void applyPolicySnapshot(UUID templateId, ApiPolicyView policySnapshot, ManagementSessionClaims session) {
        if (policySnapshot == null) {
            return;
        }
        try {
            String allowedJson = objectMapper.writeValueAsString(policySnapshot.allowedAdGroups());
            String outputFormatsJson = objectMapper.writeValueAsString(policySnapshot.outputFormats());
            String outputModesJson = objectMapper.writeValueAsString(policySnapshot.outputModes());
            Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);
            ApiPolicyEntity policy;
            if (existing.isPresent()) {
                policy = existing.get();
                policy.replaceConfiguration(
                        allowedJson,
                        policy.getDefaultRouteReleaseVersion(),
                        outputFormatsJson,
                        outputModesJson,
                        policySnapshot.batchEnabled(),
                        policySnapshot.maxBatchSize(),
                        policySnapshot.docxEncryptionEnabled(),
                        policySnapshot.pdfEncryptionEnabled(),
                        session.username()
                );
            } else {
                policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, allowedJson, session.username());
                policy.replaceConfiguration(
                        allowedJson,
                        null,
                        outputFormatsJson,
                        outputModesJson,
                        policySnapshot.batchEnabled(),
                        policySnapshot.maxBatchSize(),
                        policySnapshot.docxEncryptionEnabled(),
                        policySnapshot.pdfEncryptionEnabled(),
                        session.username()
                );
            }
            apiPolicyRepository.save(policy);
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.importFailed");
        }
    }
}
