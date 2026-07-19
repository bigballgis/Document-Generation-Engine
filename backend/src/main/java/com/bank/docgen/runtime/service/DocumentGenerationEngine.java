package com.bank.docgen.runtime.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PaginationDeltaFidelitySupport;
import com.bank.docgen.runtime.metrics.GenerationMetrics;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.service.CompositionInclusionRuleService;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VariableComputeService;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DocumentGenerationEngine {

    private final DocumentGenerationAssemblySupport assembly;
    private final GenerationMetrics generationMetrics;

    public DocumentGenerationEngine(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CompositionInclusionRuleService compositionInclusionRuleService,
            RenderProfileService renderProfileService,
            VersionFidelityWarningService versionFidelityWarningService,
            VariableComputeService variableComputeService,
            VariableSchemaValidationPort variableSchemaValidationPort,
            PaginationDeltaFidelitySupport paginationDeltaFidelitySupport,
            GenerationMetrics generationMetrics
    ) {
        this.assembly = new DocumentGenerationAssemblySupport(
                templateVersionRepository,
                anchorBindingRepository,
                masterDocumentRepository,
                masterRevisionLineRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                contentModuleReferenceService,
                compositionInclusionRuleService,
                renderProfileService,
                versionFidelityWarningService,
                variableComputeService,
                variableSchemaValidationPort,
                paginationDeltaFidelitySupport
        );
        this.generationMetrics = generationMetrics;
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                CallerRenderOverride.empty(),
                "sync",
                null,
                CompositionInclusionAxes.empty()
        );
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                "sync",
                null,
                CompositionInclusionAxes.empty()
        );
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            String mode
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                CallerRenderOverride.empty(),
                mode,
                null,
                CompositionInclusionAxes.empty()
        );
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String mode
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                mode,
                null,
                CompositionInclusionAxes.empty()
        );
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String mode,
            String localeTag
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                mode,
                localeTag,
                CompositionInclusionAxes.empty()
        );
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String mode,
            String localeTag,
            CompositionInclusionAxes inclusionAxes
    ) {
        Instant start = Instant.now();
        String format = GenerationMetrics.normalizeFormat(outputFormat);
        String invocationMode = mode == null || mode.isBlank() ? "sync" : mode;
        try {
            GeneratedDocument result = assembly.generate(
                    template,
                    releaseVersion,
                    variables,
                    outputFormat,
                    encryption,
                    callerRenderOverride,
                    localeTag,
                    inclusionAxes == null ? CompositionInclusionAxes.empty() : inclusionAxes
            );
            generationMetrics.record(Duration.between(start, Instant.now()), "success", format, invocationMode);
            return result;
        } catch (RuntimeException ex) {
            generationMetrics.record(Duration.between(start, Instant.now()), "failure", format, invocationMode);
            throw ex;
        }
    }

    public record GeneratedDocument(
            String documentId,
            String storageKey,
            byte[] artifactBytes,
            String contentType,
            String outputFormat,
            List<String> fidelityWarningCodes
    ) {
        public GeneratedDocument {
            artifactBytes = DefensiveCopies.copyBytes(artifactBytes);
            fidelityWarningCodes = DefensiveCopies.copyList(fidelityWarningCodes);
        }
    }
}
