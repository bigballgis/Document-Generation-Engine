package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DocumentArtifactPipeline {

    private final DocxEncryptionService docxEncryptionService;
    private final PdfConversionService pdfConversionService;
    private final PdfEncryptionService pdfEncryptionService;
    private final GeneratedArtifactSizeGuard artifactSizeGuard;
    private final ArtifactSpoolService artifactSpoolService;
    private final PdfConversionPostProcessor pdfConversionPostProcessor;

    public DocumentArtifactPipeline(
            DocxEncryptionService docxEncryptionService,
            PdfConversionService pdfConversionService,
            PdfEncryptionService pdfEncryptionService,
            GeneratedArtifactSizeGuard artifactSizeGuard,
            ArtifactSpoolService artifactSpoolService,
            PdfConversionPostProcessor pdfConversionPostProcessor
    ) {
        this.docxEncryptionService = docxEncryptionService;
        this.pdfConversionService = pdfConversionService;
        this.pdfEncryptionService = pdfEncryptionService;
        this.artifactSizeGuard = artifactSizeGuard;
        this.artifactSpoolService = artifactSpoolService;
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
        artifactSizeGuard.assertWithinLimit(docxBytes);
        if ("PDF".equalsIgnoreCase(outputFormat) && renderProfile != null
                && renderProfile.pdfConversionPolicy() == null) {
            throw new IllegalStateException("Render profile missing PDF conversion policy");
        }
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            PdfConversionOptions options = pdfConversionPostProcessor.resolveOptions(docxBytes, renderProfile);
            PdfConversionResult conversionResult = pdfConversionService.convertWithResult(docxBytes, options);
            byte[] pdfBytes = pdfEncryptionService.encrypt(conversionResult.pdfBytes(), encryption);
            return spoolFinalArtifact(
                    pdfBytes,
                    "application/pdf",
                    "output.pdf",
                    conversionResult.pipelineWarningCodes()
            );
        }
        byte[] encryptedDocx = docxEncryptionService.encrypt(docxBytes, encryption);
        return spoolFinalArtifact(
                encryptedDocx,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "output.docx",
                List.of()
        );
    }

    private GeneratedArtifact spoolFinalArtifact(
            byte[] finalBytes,
            String contentType,
            String storageFileName,
            List<String> pipelineWarningCodes
    ) {
        try {
            SpooledArtifact spooled = artifactSpoolService.spool(finalBytes);
            return new GeneratedArtifact(spooled, contentType, storageFileName, pipelineWarningCodes);
        } catch (java.io.IOException ex) {
            throw new RenderingOperationException(
                    "api.error.rendering.generationFailed"
            );
        }
    }

    public record GeneratedArtifact(
            SpooledArtifact spooled,
            String contentType,
            String storageFileName,
            List<String> pipelineWarningCodes
    ) implements AutoCloseable {
        public GeneratedArtifact(SpooledArtifact spooled, String contentType, String storageFileName) {
            this(spooled, contentType, storageFileName, List.of());
        }

        @Override
        public void close() throws java.io.IOException {
            spooled.close();
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
