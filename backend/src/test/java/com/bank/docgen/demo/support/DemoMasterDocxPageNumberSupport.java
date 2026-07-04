package com.bank.docgen.demo.support;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

/**
 * Shared Word field helpers for build-time demo master DOCX assets.
 */
public final class DemoMasterDocxPageNumberSupport {

    private DemoMasterDocxPageNumberSupport() {
    }

    public static void addGlobalPageNumberFields(XWPFParagraph paragraph) {
        addLiteralRun(paragraph, "Document Page ");
        addField(paragraph, " PAGE \\* MERGEFORMAT ");
        addLiteralRun(paragraph, " of ");
        addField(paragraph, " NUMPAGES \\* MERGEFORMAT ");
    }

    public static void addDualPageNumberFields(XWPFParagraph paragraph) {
        addLiteralRun(paragraph, "Section Page ");
        addField(paragraph, " PAGE \\* MERGEFORMAT ");
        addLiteralRun(paragraph, " of ");
        addField(paragraph, " SECTIONPAGES \\* MERGEFORMAT ");
        addLiteralRun(paragraph, "  |  Document Page ");
        addField(paragraph, " PAGE \\* MERGEFORMAT ");
        addLiteralRun(paragraph, " of ");
        addField(paragraph, " NUMPAGES \\* MERGEFORMAT ");
    }

    public static void addLiteralRun(XWPFParagraph paragraph, String text) {
        XWPFRun run = paragraph.createRun();
        run.setFontSize(8);
        run.setFontFamily("Calibri");
        run.setText(text);
    }

    public static void addField(XWPFParagraph paragraph, String instruction) {
        var ctp = paragraph.getCTP();
        CTR begin = ctp.addNewR();
        begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR instr = ctp.addNewR();
        instr.addNewInstrText().setStringValue(instruction);
        CTR separate = ctp.addNewR();
        separate.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR placeholder = ctp.addNewR();
        placeholder.addNewT().setStringValue("1");
        CTR end = ctp.addNewR();
        end.addNewFldChar().setFldCharType(STFldCharType.END);
    }
}
