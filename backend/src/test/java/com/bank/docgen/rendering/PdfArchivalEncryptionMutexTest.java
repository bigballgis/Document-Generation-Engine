package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-CE-O01-005 / 006 / 004 — PDF/A + encryption mutex; DOCX unaffected.
 */
@ExtendWith(MockitoExtension.class)
class PdfArchivalEncryptionMutexTest {

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
    void pdfA2bWithEncryptionEnabledRejectedBeforeConversion() {
        DocumentArtifactPipeline pipeline = pipeline();
        RenderProfile profile = archivalProfile(PdfArchivalProfile.PDF_A_2B);
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "OpenPassword12",
                "OwnerPassword12",
                List.of("ALLOW_PRINT")
        );

        assertThatThrownBy(() -> pipeline.finalizeArtifact(new byte[]{1}, "PDF", encryption, profile))
                .isInstanceOf(PdfArchivalEncryptionMutexException.class)
                .extracting(ex -> ((PdfArchivalEncryptionMutexException) ex).messageKey())
                .isEqualTo("api.error.generation.pdfArchivalEncryptionMutex");

        verify(pdfConversionService, never()).convertWithResult(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void pdfA2bWithEncryptionDisabledAllowsConversion() throws Exception {
        byte[] docx = new byte[]{1, 2, 3};
        byte[] pdf = new byte[]{37, 80, 68, 70};
        PdfConversionOptions options = PdfConversionOptions.stampingDisabled(PdfArchivalProfile.PDF_A_2B);
        when(pdfConversionPostProcessor.resolveOptions(docx, archivalProfile(PdfArchivalProfile.PDF_A_2B)))
                .thenReturn(options);
        when(pdfConversionService.convertWithResult(docx, options))
                .thenReturn(new DocumentArtifactPipeline.PdfConversionResult(pdf, List.of()));

        DocumentArtifactPipeline pipeline = pipeline();
        EncryptionOptionsView encryption = new EncryptionOptionsView(false, null, null, null);

        try (DocumentArtifactPipeline.GeneratedArtifact artifact =
                pipeline.finalizeArtifact(docx, "PDF", encryption, archivalProfile(PdfArchivalProfile.PDF_A_2B))) {
            assertThat(artifact.contentType()).isEqualTo("application/pdf");
            verify(pdfConversionService).convertWithResult(docx, options);
        }
    }

    @Test
    void docxIgnoresArchivalMutexEvenWithEncryption() throws Exception {
        DocumentArtifactPipeline pipeline = pipeline();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "OpenPassword12",
                null,
                null
        );

        try (DocumentArtifactPipeline.GeneratedArtifact artifact = pipeline.finalizeArtifact(
                buildMinimalDocxBytes(),
                "DOCX",
                encryption,
                archivalProfile(PdfArchivalProfile.PDF_A_2B)
        )) {
            assertThat(artifact.contentType())
                    .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            verify(pdfConversionService, never()).convertWithResult(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any());
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

    private static RenderProfile archivalProfile(PdfArchivalProfile archival) {
        return new RenderProfile(
                "rp-v1",
                "MASTER_CATALOG_LOCKED",
                "CONTROLLED_MULTILEVEL",
                "REPEAT_HEADER",
                "PROPORTIONAL_FIT",
                "SEMANTIC_FIDELITY",
                "BLOCKERS_PREVENT_PUBLISH",
                false,
                archival
        );
    }

    private static byte[] buildMinimalDocxBytes() throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(output)) {
            zip.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/word/document.xml"
                        ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                    </Types>
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
            zip.write("""
                    <?xml version="1.0" encoding="UTF-8"?>
                    <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                      <w:body><w:p><w:r><w:t>body</w:t></w:r></w:p></w:body>
                    </w:document>
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
