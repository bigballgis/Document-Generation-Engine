package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentArtifactPipelineTest {

    @Mock
    private PdfConversionService pdfConversionService;

    private final DocxEncryptionService docxEncryptionService = new DocxEncryptionService();
    private final PdfEncryptionService pdfEncryptionService = new PdfEncryptionService();

    @Test
    void pdfPathConvertsThenEncrypts() {
        byte[] docx = new byte[]{1, 2, 3};
        byte[] pdf = new byte[]{37, 80, 68, 70};
        when(pdfConversionService.convert(docx)).thenReturn(pdf);

        DocumentArtifactPipeline pipeline = new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryptionService
        );
        EncryptionOptionsView encryption = new EncryptionOptionsView(false, null, null, null);

        DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "PDF", encryption);

        verify(pdfConversionService).convert(docx);
        assertThat(artifact.contentType()).isEqualTo("application/pdf");
        assertThat(artifact.storageFileName()).isEqualTo("output.pdf");
        assertThat(artifact.bytes()).isEqualTo(pdf);
    }

    @Test
    void docxPathEncryptsWithoutPdfConversion() {
        byte[] docx = buildMinimalDocxBytes();
        DocumentArtifactPipeline pipeline = new DocumentArtifactPipeline(
                docxEncryptionService,
                pdfConversionService,
                pdfEncryptionService
        );
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                null,
                null
        );

        DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "DOCX", encryption);

        assertThat(artifact.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(artifact.storageFileName()).isEqualTo("output.docx");
        assertThat(artifact.bytes()).isNotEqualTo(docx);
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
