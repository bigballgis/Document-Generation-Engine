package com.bank.docgen.rendering;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Applies literal {@code SPECIMEN} text to DOCX header and footer parts for preview / test-generate
 * artifacts (CE-G02). Fail-closed on IO errors.
 */
public final class DocxSpecimenWatermarkStamper {

    public static final String WATERMARK_TEXT = "SPECIMEN";

    private DocxSpecimenWatermarkStamper() {
    }

    public static byte[] apply(byte[] docxBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ensureSpecimenInHeaders(document);
            ensureSpecimenInFooters(document);
            document.write(output);
            return output.toByteArray();
        } catch (IOException | RuntimeException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private static void ensureSpecimenInHeaders(XWPFDocument document) {
        List<XWPFHeader> headers = document.getHeaderList();
        if (headers == null || headers.isEmpty()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            appendSpecimenParagraph(header.createParagraph());
            return;
        }
        for (XWPFHeader header : headers) {
            if (!containsSpecimen(header.getParagraphs())) {
                appendSpecimenParagraph(header.createParagraph());
            }
        }
    }

    private static void ensureSpecimenInFooters(XWPFDocument document) {
        List<XWPFFooter> footers = document.getFooterList();
        if (footers == null || footers.isEmpty()) {
            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            appendSpecimenParagraph(footer.createParagraph());
            return;
        }
        for (XWPFFooter footer : footers) {
            if (!containsSpecimen(footer.getParagraphs())) {
                appendSpecimenParagraph(footer.createParagraph());
            }
        }
    }

    private static boolean containsSpecimen(List<XWPFParagraph> paragraphs) {
        if (paragraphs == null) {
            return false;
        }
        for (XWPFParagraph paragraph : paragraphs) {
            String text = paragraph.getText();
            if (text != null && text.contains(WATERMARK_TEXT)) {
                return true;
            }
        }
        return false;
    }

    private static void appendSpecimenParagraph(XWPFParagraph paragraph) {
        XWPFRun run = paragraph.createRun();
        run.setText(WATERMARK_TEXT);
        run.setBold(true);
    }
}
