package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PdfSpecimenWatermarkStamper;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreviewArtifactDownloadService {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewArtifactDownloadService.class);
    private static final EncryptionOptionsView NO_ENCRYPTION =
            new EncryptionOptionsView(false, null, null, null);

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final PreviewRecordRepository previewRecordRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final RenderProfileService renderProfileService;

    public PreviewArtifactDownloadService(
            TemplatePreviewAuthorizationPort previewAuthorizationPort,
            PreviewRecordRepository previewRecordRepository,
            TemplateVersionRepository templateVersionRepository,
            ObjectStoragePort objectStoragePort,
            DocumentArtifactPipeline documentArtifactPipeline,
            RenderProfileService renderProfileService
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.previewRecordRepository = previewRecordRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.objectStoragePort = objectStoragePort;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.renderProfileService = renderProfileService;
    }

    @Transactional
    public PreviewDownloadArtifact openDownload(
            UUID templateId,
            UUID previewId,
            PreviewArtifactFormat format,
            ManagementSessionClaims session
    ) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        PreviewRecordEntity preview = requirePreview(templateId, previewId);
        if (preview.getStatus() != PreviewStatus.SUCCEEDED) {
            throw new PreviewArtifactNotAvailableException();
        }

        return switch (format) {
            case DOCX -> openDocxDownload(preview);
            case PDF -> openPdfDownload(preview);
        };
    }

    private PreviewDownloadArtifact openDocxDownload(PreviewRecordEntity preview) {
        String storageKey = preview.getArtifactStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            throw new PreviewArtifactNotAvailableException();
        }
        return artifactFromStorage(
                storageKey,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                previewFilename(preview, "docx")
        );
    }

    private PreviewDownloadArtifact openPdfDownload(PreviewRecordEntity preview) {
        String storageKey = preview.getPdfArtifactStorageKey();
        if (storageKey == null || storageKey.isBlank()) {
            storageKey = materializePdfArtifact(preview);
        }
        return artifactFromStorage(storageKey, "application/pdf", previewFilename(preview, "pdf"));
    }

    private String materializePdfArtifact(PreviewRecordEntity preview) {
        String docxKey = preview.getArtifactStorageKey();
        if (docxKey == null || docxKey.isBlank()) {
            throw new PreviewArtifactNotAvailableException();
        }
        TemplateVersionEntity version = templateVersionRepository.findById(preview.getTemplateVersionId())
                .orElseThrow(PreviewArtifactNotAvailableException::new);
        RenderProfile renderProfile = renderProfileService.resolveEffectiveProfile(
                version,
                CallerRenderOverride.empty()
        );
        byte[] docxBytes;
        try (InputStream docxStream = objectStoragePort.get(docxKey)) {
            docxBytes = docxStream.readAllBytes();
        } catch (IOException ex) {
            LOG.warn("Failed to read preview DOCX artifact {}: {}", docxKey, ex.getMessage());
            throw new PreviewArtifactNotAvailableException();
        }
        DocumentArtifactPipeline.GeneratedArtifact pdfArtifact = documentArtifactPipeline.finalizeArtifact(
                docxBytes,
                "PDF",
                NO_ENCRYPTION,
                renderProfile
        );
        String pdfKey = "previews/" + preview.getId() + "/output.pdf";
        try (pdfArtifact) {
            byte[] pdfBytes;
            try (java.io.InputStream pdfStream = pdfArtifact.spooled().openInputStream()) {
                pdfBytes = pdfStream.readAllBytes();
            }
            // CE-G02: lazy PDF materialization must still apply diagonal SPECIMEN (fail-closed).
            pdfBytes = PdfSpecimenWatermarkStamper.apply(pdfBytes);
            objectStoragePort.put(
                    pdfKey,
                    new java.io.ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    pdfArtifact.contentType()
            );
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Failed to materialize preview PDF for {}: {}", preview.getId(), ex.getMessage());
            throw new PreviewArtifactNotAvailableException();
        }
        preview.setPdfArtifactStorageKey(pdfKey);
        previewRecordRepository.save(preview);
        return pdfKey;
    }

    private PreviewDownloadArtifact artifactFromStorage(
            String storageKey,
            String contentType,
            String filename
    ) {
        if (!objectStoragePort.exists(storageKey)) {
            throw new PreviewArtifactNotAvailableException();
        }
        InputStream stream = objectStoragePort.get(storageKey);
        return new PreviewDownloadArtifact(stream, contentType, filename);
    }

    private PreviewRecordEntity requirePreview(UUID templateId, UUID previewId) {
        PreviewRecordEntity preview = previewRecordRepository.findById(previewId)
                .orElseThrow(PreviewNotFoundException::new);
        if (!preview.getTemplateId().equals(templateId)) {
            throw new PreviewNotFoundException();
        }
        return preview;
    }

    private String previewFilename(PreviewRecordEntity preview, String extension) {
        String datasetSuffix = preview.getTestDataSetExternalId() == null
                || preview.getTestDataSetExternalId().isBlank()
                ? "adhoc"
                : preview.getTestDataSetExternalId().toLowerCase(Locale.ROOT);
        return "preview-" + datasetSuffix + "-" + preview.getId().toString().substring(0, 8) + "." + extension;
    }

    public enum PreviewArtifactFormat {
        DOCX,
        PDF
    }

    public record PreviewDownloadArtifact(
            InputStream contentStream,
            String contentType,
            String filename
    ) implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            contentStream.close();
        }
    }
}
