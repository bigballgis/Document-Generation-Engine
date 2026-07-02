package com.bank.docgen.master.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class DocxAnchorExtractorTest {

    @Test
    void extractsAnchorPlaceholdersFromDocx() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Hello {{anchor:HEADER}} and {{anchor:BODY_MAIN}}");
            document.write(output);
            docxBytes = output.toByteArray();
        }

        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        var anchorIds = extractor.extractAnchorIds(new ByteArrayInputStream(docxBytes));

        assertThat(anchorIds).containsExactly("HEADER", "BODY_MAIN");
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(docxBytes)))
                .containsExactly("HEADER", "BODY_MAIN");
    }

    @Test
    void extractsAnchorBookmarkNamesFromDocx() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.getCTP().addNewBookmarkStart().setId(BigInteger.valueOf(1));
            paragraph.getCTP().getBookmarkStartArray(0).setName("anchor.FOOTER_NOTE");
            paragraph.getCTP().addNewBookmarkEnd().setId(BigInteger.valueOf(1));
            document.write(output);
            docxBytes = output.toByteArray();
        }

        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        var anchorIds = extractor.extractAnchorIds(new ByteArrayInputStream(docxBytes));

        assertThat(anchorIds).containsExactly("FOOTER_NOTE");
    }

    @Test
    void extractOrderedAnchorIdsTraversesHeadersBeforeBodyBeforeFooters() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createFooter(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("{{anchor:FOOTER_ANCHOR}}");
            XWPFParagraph bodyParagraph = document.createParagraph();
            bodyParagraph.createRun().setText("{{anchor:BODY_ANCHOR}}");
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun()
                    .setText("{{anchor:HEADER_ANCHOR}}");
            document.write(output);
            docxBytes = output.toByteArray();
        }

        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractOrderedAnchorIds(new ByteArrayInputStream(docxBytes)))
                .containsExactly("HEADER_ANCHOR", "BODY_ANCHOR", "FOOTER_ANCHOR");
    }
}
