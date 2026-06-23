package com.bank.docgen.runtime.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentGenerationEngine {

    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;

    public DocumentGenerationEngine(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption
    ) {
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(TemplateNotFoundException::new);
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> bindingJson = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        Map<String, String> anchorContent = docxAssembler.buildAnchorReplacements(bindingJson, variables);
        byte[] docx;
        try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
            docx = docxAssembler.assemble(masterStream, anchorContent);
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
        DocumentArtifactPipeline.GeneratedArtifact artifact = documentArtifactPipeline.finalizeArtifact(
                docx,
                outputFormat,
                encryption
        );
        String documentId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String storageKey = "generated/" + documentId + "/" + artifact.storageFileName();
        objectStoragePort.put(
                storageKey,
                new java.io.ByteArrayInputStream(artifact.bytes()),
                artifact.bytes().length,
                artifact.contentType()
        );
        return new GeneratedDocument(
                documentId,
                storageKey,
                artifact.bytes(),
                artifact.contentType(),
                outputFormat,
                List.of(FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name())
        );
    }

    public record GeneratedDocument(
            String documentId,
            String storageKey,
            byte[] artifactBytes,
            String contentType,
            String outputFormat,
            List<String> fidelityWarningCodes
    ) {
    }
}
