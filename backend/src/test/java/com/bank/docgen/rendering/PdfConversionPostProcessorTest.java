package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PdfConversionPostProcessorTest {

    @Test
    void leavesPdfUntouchedWhenStampingDisabled() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(false);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        byte[] sourcePdf = samplePdf();
        PdfConversionOptions options = PdfConversionOptions.stampingDisabled();

        PdfPageStampResult finished = processor.finishPdf(sourcePdf, options);

        try (PDDocument document = Loader.loadPDF(finished.pdfBytes())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Body page 1");
            assertThat(text).doesNotContain("Page 1 of 1");
        }
        assertThat(finished.warning()).isEmpty();
    }

    @Test
    void stampsPdfWhenStampingEnabled() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        byte[] sourcePdf = samplePdf();
        PdfConversionOptions options = PdfConversionOptions.stampingEnabled(PdfPageNumberStampPlan.globalOnly());

        PdfPageStampResult finished = processor.finishPdf(sourcePdf, options);

        try (PDDocument document = Loader.loadPDF(finished.pdfBytes())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Page 1 of 1");
        }
        assertThat(finished.warning()).isEmpty();
    }

    @Test
    void renderProfileTrue_enablesStampingRegardlessOfGlobal() throws Exception {
        // BDD-CE-K06c-004
        assertThat(isStampingEnabled(true, false)).isTrue();
        assertThat(isStampingEnabled(true, true)).isTrue();
    }

    @Test
    void renderProfileFalse_disablesStampingEvenWhenGlobalTrue() throws Exception {
        // BDD-CE-K06c-005 — global must not OR/bypass locked profile false
        assertThat(isStampingEnabled(false, true)).isFalse();
    }

    @Test
    void renderProfileFalse_andGlobalFalse_disablesStamping() throws Exception {
        // BDD-CE-K06c-006
        assertThat(isStampingEnabled(false, false)).isFalse();
    }

    @Test
    void nullRenderProfile_fallsBackToGlobal() throws Exception {
        // BDD-CE-K06c-007
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        assertThat(processor.isStampingEnabled((com.bank.docgen.sharedkernel.document.RenderProfile) null))
                .isTrue();

        properties.setPdfPageNumberStampingEnabled(false);
        assertThat(processor.isStampingEnabled((com.bank.docgen.sharedkernel.document.RenderProfile) null))
                .isFalse();
    }

    @Test
    void resolveOptions_respectsProfileFalseDespiteGlobalTrue() throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        PdfConversionOptions options = processor.resolveOptions(
                new byte[0],
                profile(false)
        );
        assertThat(options.pageNumberStampingEnabled()).isFalse();
    }

    @Test
    void finishPdf_respectsResolvedOptionsDisabledDespiteGlobalTrue() throws Exception {
        // BDD-CE-K06c-005 / K06c-C7 — production path: resolveOptions → finishPdf
        // must not re-enable stamping via global OR when profile locked false
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        PdfConversionOptions options = processor.resolveOptions(new byte[0], profile(false));

        assertThat(options.pageNumberStampingEnabled()).isFalse();
        assertThat(processor.isStampingEnabled(options)).isFalse();

        byte[] sourcePdf = samplePdf();
        PdfPageStampResult finished = processor.finishPdf(sourcePdf, options);

        try (PDDocument document = Loader.loadPDF(finished.pdfBytes())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Body page 1");
            assertThat(text).doesNotContain("Page 1 of 1");
        }
        assertThat(finished.warning()).isEmpty();
    }

    @Test
    void prepareDocxForConversion_skipsWhenResolvedOptionsDisabledDespiteGlobalTrue() throws Exception {
        // BDD-CE-K06c-005 — prepareDocxForConversion must trust resolveOptions, not global
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(true);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        PdfConversionOptions options = processor.resolveOptions(new byte[0], profile(false));
        byte[] sourceDocx = new byte[]{0x50, 0x4B, 0x03, 0x04};

        byte[] prepared = processor.prepareDocxForConversion(sourceDocx, options);

        assertThat(processor.isStampingEnabled(options)).isFalse();
        assertThat(prepared).isSameAs(sourceDocx);
    }

    private static boolean isStampingEnabled(boolean profileEnabled, boolean globalEnabled) throws Exception {
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setPdfPageNumberStampingEnabled(globalEnabled);
        PdfConversionPostProcessor processor = new PdfConversionPostProcessor(
                properties,
                new DocxPdfConversionPreprocessor()
        );
        return processor.isStampingEnabled(profile(profileEnabled));
    }

    private static com.bank.docgen.sharedkernel.document.RenderProfile profile(boolean enabled)
            throws Exception {
        return com.bank.docgen.sharedkernel.document.RenderProfile.fromJsonNode(
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                        "{\"pdfPageNumberStampingEnabled\": " + enabled + "}"
                )
        );
    }

    private static byte[] samplePdf() throws Exception {
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            source.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream contentStream = new PDPageContentStream(source, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Body page 1");
                contentStream.endText();
            }
            source.save(output);
            return output.toByteArray();
        }
    }
}
