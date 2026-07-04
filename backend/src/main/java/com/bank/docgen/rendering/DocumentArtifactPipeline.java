package com.bank.docgen.rendering;

import com.bank.docgen.authoring.structured.RenderProfile;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import org.springframework.stereotype.Service;

@Service
public class DocumentArtifactPipeline {

    private final DocxEncryptionService docxEncryptionService;
    private final PdfConversionService pdfConversionService;
    private final PdfEncryptionService pdfEncryptionService;
    private final GeneratedArtifactSizeGuard artifactSizeGuard;
    private final ArtifactSpoolService artifactSpoolService;

    public DocumentArtifactPipeline(
            DocxEncryptionService docxEncryptionService,
            PdfConversionService pdfConversionService,
            PdfEncryptionService pdfEncryptionService,
            GeneratedArtifactSizeGuard artifactSizeGuard,
            ArtifactSpoolService artifactSpoolService
    ) {
        this.docxEncryptionService = docxEncryptionService;
        this.pdfConversionService = pdfConversionService;
        this.pdfEncryptionService = pdfEncryptionService;
        this.artifactSizeGuard = artifactSizeGuard;
        this.artifactSpoolService = artifactSpoolService;
    }

    public GeneratedArtifact finalizeArtifact(
            byte[] docxBytes,
            String outputFormat,
            EncryptionOptionsView encryption
    ) {
        return finalizeArtifact(docxBytes, outputFormat, encryption, null);
    }

    public GeneratedArtifact finalizeArtifact(
            byte[] docxBytes,
            String outputFormat,
            EncryptionOptionsView encryption,
            RenderProfile renderProfile
    ) {
        artifactSizeGuard.assertWithinLimit(docxBytes);
        if ("PDF".equalsIgnoreCase(outputFormat) && renderProfile != null
                && renderProfile.pdfConversionPolicy() == null) {
            throw new IllegalStateException("Render profile missing PDF conversion policy");
        }
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            byte[] pdfBytes = pdfConversionService.convert(docxBytes);
            pdfBytes = pdfEncryptionService.encrypt(pdfBytes, encryption);
            return spoolFinalArtifact(
                    pdfBytes,
                    "application/pdf",
                    "output.pdf"
            );
        }
        byte[] encryptedDocx = docxEncryptionService.encrypt(docxBytes, encryption);
        return spoolFinalArtifact(
                encryptedDocx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );
    }

    private GeneratedArtifact spoolFinalArtifact(byte[] finalBytes, String contentType, String storageFileName) {
        try {
            SpooledArtifact spooled = artifactSpoolService.spool(finalBytes);
            return new GeneratedArtifact(spooled, contentType, storageFileName);
        } catch (java.io.IOException ex) {
            throw new com.bank.docgen.template.service.TemplateValidationException(
                    "api.error.rendering.generationFailed"
            );
        }
    }

    public record GeneratedArtifact(
            SpooledArtifact spooled,
            String contentType,
            String storageFileName
    ) implements AutoCloseable {
        @Override
        public void close() throws java.io.IOException {
            spooled.close();
        }
    }
}
