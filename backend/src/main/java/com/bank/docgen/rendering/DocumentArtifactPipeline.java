package com.bank.docgen.rendering;

import com.bank.docgen.authoring.structured.RenderProfile;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import org.springframework.stereotype.Service;

@Service
public class DocumentArtifactPipeline {

    private final DocxEncryptionService docxEncryptionService;
    private final PdfConversionService pdfConversionService;
    private final PdfEncryptionService pdfEncryptionService;

    public DocumentArtifactPipeline(
            DocxEncryptionService docxEncryptionService,
            PdfConversionService pdfConversionService,
            PdfEncryptionService pdfEncryptionService
    ) {
        this.docxEncryptionService = docxEncryptionService;
        this.pdfConversionService = pdfConversionService;
        this.pdfEncryptionService = pdfEncryptionService;
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
        if ("PDF".equalsIgnoreCase(outputFormat) && renderProfile != null
                && renderProfile.pdfConversionPolicy() == null) {
            throw new IllegalStateException("Render profile missing PDF conversion policy");
        }
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            byte[] pdfBytes = pdfConversionService.convert(docxBytes);
            pdfBytes = pdfEncryptionService.encrypt(pdfBytes, encryption);
            return new GeneratedArtifact(
                    pdfBytes,
                    "application/pdf",
                    "output.pdf"
            );
        }
        byte[] encryptedDocx = docxEncryptionService.encrypt(docxBytes, encryption);
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
