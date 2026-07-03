package com.bank.docgen.rendering;

import com.bank.docgen.authoring.structured.RenderProfile;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentArtifactPipeline {

    private final DocxEncryptionService docxEncryptionService;
    private final PdfConversionService pdfConversionService;
    private final PdfEncryptionService pdfEncryptionService;
    private final PdfConversionPostProcessor pdfConversionPostProcessor;

    public DocumentArtifactPipeline(
            DocxEncryptionService docxEncryptionService,
            PdfConversionService pdfConversionService,
            PdfEncryptionService pdfEncryptionService,
            PdfConversionPostProcessor pdfConversionPostProcessor
    ) {
        this.docxEncryptionService = docxEncryptionService;
        this.pdfConversionService = pdfConversionService;
        this.pdfEncryptionService = pdfEncryptionService;
        this.pdfConversionPostProcessor = pdfConversionPostProcessor;
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
            PdfConversionOptions options = pdfConversionPostProcessor.resolveOptions(docxBytes, renderProfile);
            PdfConversionResult conversionResult = pdfConversionService.convertWithResult(docxBytes, options);
            byte[] pdfBytes = pdfEncryptionService.encrypt(conversionResult.pdfBytes(), encryption);
            return new GeneratedArtifact(
                    pdfBytes,
                    "application/pdf",
                    "output.pdf",
                    conversionResult.pipelineWarningCodes()
            );
        }
        byte[] encryptedDocx = docxEncryptionService.encrypt(docxBytes, encryption);
        return new GeneratedArtifact(
                encryptedDocx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx",
                List.of()
        );
    }

    public record GeneratedArtifact(
            byte[] bytes,
            String contentType,
            String storageFileName,
            List<String> pipelineWarningCodes
    ) {
        public GeneratedArtifact(byte[] bytes, String contentType, String storageFileName) {
            this(bytes, contentType, storageFileName, List.of());
        }
    }

    public record PdfConversionResult(
            byte[] pdfBytes,
            List<String> pipelineWarningCodes
    ) {

        public static PdfConversionResult of(byte[] pdfBytes, PdfPageStampResult stampResult) {
            List<String> warnings = new ArrayList<>();
            stampResult.warning().ifPresent(code -> warnings.add(code.name()));
            return new PdfConversionResult(pdfBytes, List.copyOf(warnings));
        }
    }
}
