package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

class DocxPdfConversionPreprocessorTest {

    private final DocxPdfConversionPreprocessor preprocessor = new DocxPdfConversionPreprocessor();

    @Test
    void removesFooterPageFieldOnlyFromPdfConversionCopy() throws Exception {
        byte[] sourceDocx = buildDocxWithFooterPageField();

        byte[] prepared = preprocessor.prepareForPdfConversion(sourceDocx);

        assertThat(footerContainsPageField(sourceDocx)).isTrue();
        assertThat(footerContainsPageField(prepared)).isFalse();
        assertThat(footerContainsDisclaimer(prepared)).isTrue();
    }

    @Test
    void detectsPageFieldInstruction() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFFooter footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            XWPFParagraph pageLine = footer.createParagraph();
            addPageField(pageLine);

            assertThat(preprocessor.containsPageField(pageLine)).isTrue();
        }
    }

    private static byte[] buildDocxWithFooterPageField() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Body content");
            XWPFFooter footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            footer.createParagraph().createRun().setText("Footer address line");
            XWPFParagraph pageLine = footer.createParagraph();
            pageLine.setAlignment(ParagraphAlignment.CENTER);
            addPageField(pageLine);
            XWPFParagraph disclaimer = footer.createParagraph();
            disclaimer.createRun().setText("Disclaimer line");
            document.write(output);
            return output.toByteArray();
        }
    }

    private static void addPageField(XWPFParagraph paragraph) {
        paragraph.createRun().setText("Page ");
        var ctp = paragraph.getCTP();
        CTR begin = ctp.addNewR();
        begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR instruction = ctp.addNewR();
        instruction.addNewInstrText().setStringValue(" PAGE \\* MERGEFORMAT ");
        CTR separate = ctp.addNewR();
        separate.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR placeholder = ctp.addNewR();
        placeholder.addNewT().setStringValue("1");
        CTR end = ctp.addNewR();
        end.addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private static boolean footerContainsPageField(byte[] docxBytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            for (XWPFFooter footer : document.getFooterList()) {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    for (CTR run : paragraph.getCTP().getRList()) {
                        if (!run.getInstrTextList().isEmpty()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean footerContainsDisclaimer(byte[] docxBytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            return document.getFooterList().stream()
                    .flatMap(footer -> footer.getParagraphs().stream())
                    .map(XWPFParagraph::getText)
                    .anyMatch(text -> text != null && text.contains("Disclaimer line"));
        }
    }
}
