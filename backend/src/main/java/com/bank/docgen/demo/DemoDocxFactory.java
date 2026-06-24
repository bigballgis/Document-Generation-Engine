package com.bank.docgen.demo;

import java.io.ByteArrayOutputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

final class DemoDocxFactory {

    private DemoDocxFactory() {}

    static byte[] buildHeaderAnchorDocx(String anchorId) {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText("Dear {{anchor:" + anchorId + "}} customer");
            document.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build demo DOCX", ex);
        }
    }
}
