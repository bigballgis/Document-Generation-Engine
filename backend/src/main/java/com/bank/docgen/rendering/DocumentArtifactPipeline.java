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

    public DocumentArtifactPipeline(
            DocxEncryptionService docxEncryptionService,
            PdfConversionService pdfConversionService,
            PdfEncryptionService pdfEncryptionService,
            GeneratedArtifactSizeGuard artifactSizeGuard
    ) {
        this.docxEncryptionService = docxEncryptionService;
        this.pdfConversionService = pdfConversionService;
        this.pdfEncryptionService = pdfEncryptionService;
        this.artifactSizeGuard = artifactSizeGuard;
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
            artifactSizeGuard.assertWithinLimit(pdfBytes);
            return new GeneratedArtifact(
                    pdfBytes,
                    "application/pdf",
                    "output.pdf"
            );
        }
        byte[] encryptedDocx = docxEncryptionService.encrypt(docxBytes, encryption);
        artifactSizeGuard.assertWithinLimit(encryptedDocx);
        return new GeneratedArtifact(
                encryptedDocx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx"
        );
    }

    public record GeneratedArtifact(
            byte[] bytes,
            String contentType,
            String storageFileName
    ) {
    }
}
