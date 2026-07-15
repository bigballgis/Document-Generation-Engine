package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentArtifactPipelineTest {

    @Mock
    private PdfConversionService pdfConversionService;

    @Mock
    private PdfConversionPostProcessor pdfConversionPostProcessor;

    private final DocxEncryptionService docxEncryptionService = new DocxEncryptionService();
    private final PdfEncryptionService pdfEncryptionService = new PdfEncryptionService();
    private final GeneratedArtifactSizeGuard artifactSizeGuard = new GeneratedArtifactSizeGuard(
            new DocgenRenderingProperties()
    );
    private final ArtifactSpoolService artifactSpoolService = new ArtifactSpoolService(artifactSizeGuard);

    @Test
    void pdfPathConvertsThenEncrypts() throws Exception {
        byte[] docx = new byte[]{1, 2, 3};
        byte[] pdf = new byte[]{37, 80, 68, 70};
        PdfConversionOptions options = PdfConversionOptions.stampingDisabled();
        when(pdfConversionPostProcessor.resolveOptions(docx, null)).thenReturn(options);
        when(pdfConversionService.convertWithResult(docx, options))
                .thenReturn(new DocumentArtifactPipeline.PdfConversionResult(pdf, List.of()));

        DocumentArtifactPipeline pipeline = new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryptionService,
                artifactSizeGuard,
                artifactSpoolService,
                pdfConversionPostProcessor
        );
        EncryptionOptionsView encryption = new EncryptionOptionsView(false, null, null, null);

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "PDF", encryption)) {
            verify(pdfConversionService).convertWithResult(docx, options);
            assertThat(artifact.contentType()).isEqualTo("application/pdf");
            assertThat(artifact.storageFileName()).isEqualTo("output.pdf");
            assertThat(artifact.spooled().openInputStream().readAllBytes()).isEqualTo(pdf);
        }
    }

    @Test
    void docxPathEncryptsWithoutPdfConversion() throws Exception {
        byte[] docx = buildMinimalDocxBytes();
        DocumentArtifactPipeline pipeline = new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryptionService,
                artifactSizeGuard,
                artifactSpoolService,
                pdfConversionPostProcessor
        );
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                null,
                null
        );

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "DOCX", encryption)) {
            assertThat(artifact.contentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            assertThat(artifact.storageFileName()).isEqualTo("output.docx");
            assertThat(artifact.spooled().openInputStream().readAllBytes()).isNotEqualTo(docx);
        }
    }

    @Test
    void bddCeC06_002_docxWithPermissionsEmitsDocxPermissionsNotAppliedWarning() throws Exception {
        byte[] docx = buildMinimalDocxBytes();
        DocumentArtifactPipeline pipeline = pipeline();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                "OwnerPass12345",
                List.of("ALLOW_PRINT", "ALLOW_COPY")
        );

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "DOCX", encryption)) {
            assertThat(artifact.pipelineWarningCodes())
                    .containsExactly(FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name());
            assertThat(artifact.spooled().openInputStream().readAllBytes()).isNotEqualTo(docx);
        }
    }

    @Test
    void bddCeC06_003_pdfWithPermissionsDoesNotEmitCeC06Warning() throws Exception {
        byte[] docx = new byte[]{1, 2, 3};
        byte[] pdf = new byte[]{37, 80, 68, 70};
        PdfConversionOptions options = PdfConversionOptions.stampingDisabled();
        when(pdfConversionPostProcessor.resolveOptions(docx, null)).thenReturn(options);
        when(pdfConversionService.convertWithResult(docx, options))
                .thenReturn(new DocumentArtifactPipeline.PdfConversionResult(pdf, List.of()));

        PdfEncryptionService pdfEncryption = mock(PdfEncryptionService.class);
        when(pdfEncryption.encrypt(any(), any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentArtifactPipeline pipeline = new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryption,
                artifactSizeGuard,
                artifactSpoolService,
                pdfConversionPostProcessor
        );
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                "OwnerPass12345",
                List.of("ALLOW_PRINT")
        );

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "PDF", encryption)) {
            assertThat(artifact.pipelineWarningCodes())
                    .doesNotContain(FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name());
        }
    }

    @Test
    void bddCeC06_004_docxWithoutPermissionsEmitsNoCeC06Warning() throws Exception {
        byte[] docx = buildMinimalDocxBytes();
        DocumentArtifactPipeline pipeline = pipeline();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                null,
                List.of()
        );

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "DOCX", encryption)) {
            assertThat(artifact.pipelineWarningCodes())
                    .doesNotContain(FidelityWarningCode.DOCX_PERMISSIONS_NOT_APPLIED.name());
        }
    }

    private DocumentArtifactPipeline pipeline() {
        return new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryptionService,
                artifactSizeGuard,
                artifactSpoolService,
                pdfConversionPostProcessor
        );
    }

    private byte[] buildMinimalDocxBytes() {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph = document.createParagraph();
            org.apache.poi.xwpf.usermodel.XWPFRun run = paragraph.createRun();
            run.setText("sample");
            document.write(output);
            return output.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
