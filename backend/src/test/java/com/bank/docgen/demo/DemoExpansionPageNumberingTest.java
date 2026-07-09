package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.rendering.PdfPageNumberStampPlan;
import com.bank.docgen.rendering.PdfPageNumberStamper;
import com.bank.docgen.rendering.PdfPageStampResult;
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

/**
 * BDD-DEMO-EXP-005 / BDD-DEMO-EXP-006 — dual page numbering in DOCX master assets and PDF stamping.
 */
class DemoExpansionPageNumberingTest {

    @Test
    void bddDemoExp005_dualPageFooterFieldsPresentInFolMasterDocx() throws Exception {
        byte[] docx = FolMasterDocxAssetGeneratorTest.buildWholesaleFolMasterDocx();
        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("SECTIONPAGES");
        assertThat(footerXml).contains("NUMPAGES");
    }

    @Test
    void bddDemoExp006_pdfDualPageStampingMatchesDocxSemantics() throws Exception {
        byte[] sourcePdf = threePagePdf();
        PdfPageNumberStampPlan plan = PdfPageNumberStampPlan.sectionAndGlobal(java.util.List.of(1, 2));

        PdfPageStampResult result = PdfPageNumberStamper.stampPageNumbers(sourcePdf, plan);

        assertThat(result.warning()).isEmpty();
        try (PDDocument document = Loader.loadPDF(result.pdfBytes())) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Section Page 1 of 1  |  Document Page 1 of 3");
            assertThat(text).contains("Section Page 1 of 2  |  Document Page 2 of 3");
            assertThat(text).contains("Section Page 2 of 2  |  Document Page 3 of 3");
        }
    }

    @Test
    void pdfStampFailureEmitsFidelityWarningInsteadOfSilentNoPagePdf() {
        byte[] invalidPdf = new byte[]{0x00, 0x01, 0x02};

        PdfPageStampResult result = PdfPageNumberStamper.stampPageNumbers(invalidPdf, PdfPageNumberStampPlan.globalOnly());

        assertThat(result.pdfBytes()).isEqualTo(invalidPdf);
        assertThat(result.warning()).contains(FidelityWarningCode.PDF_PAGE_NUMBER_STAMP_FAILED);
    }

    private static byte[] threePagePdf() throws Exception {
        try (PDDocument source = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int page = 1; page <= 3; page++) {
                PDPage pdPage = new PDPage(PDRectangle.A4);
                source.addPage(pdPage);
                try (PDPageContentStream stream = new PDPageContentStream(source, pdPage)) {
                    stream.beginText();
                    stream.setFont(font, 12);
                    stream.newLineAtOffset(50, 750);
                    stream.showText("Body " + page);
                    stream.endText();
                }
            }
            source.save(output);
            return output.toByteArray();
        }
    }
}
