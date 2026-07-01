package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;

/**
 * Build-time helper only — writes the wholesale FOL master DOCX asset under {@code deploy/demo-fol/assets/}.
 * The master is a layout container: page margins, headers, footers, and section-level anchors (ADR-0019).
 */
class FolMasterDocxAssetGeneratorTest {

    /** Bump when page layout / header / footer changes; import script uses this to refresh uploaded masters. */
    static final String MASTER_LAYOUT_VERSION = "fol-layout-v2-headers-footers-margins";

    private static final Path ASSET_PATH = Path.of("..", "deploy", "demo-fol", "assets", "wholesale-fol-master.docx");

    static final List<String> ANCHOR_IDS = List.of(
            "FOL_HEADER",
            "FOL_FACILITY_SUMMARY",
            "FOL_SEC_01", "FOL_SEC_02", "FOL_SEC_03", "FOL_SEC_04", "FOL_SEC_05",
            "FOL_SEC_06", "FOL_SEC_07", "FOL_SEC_08", "FOL_SEC_09", "FOL_SEC_10",
            "FOL_SEC_11", "FOL_SEC_12", "FOL_SEC_13", "FOL_SEC_14", "FOL_SEC_15",
            "FOL_SEC_16", "FOL_SEC_17", "FOL_SEC_18", "FOL_SEC_19", "FOL_SEC_20",
            "FOL_SEC_21", "FOL_SEC_22", "FOL_SEC_23", "FOL_SEC_24", "FOL_SEC_25",
            "FOL_SEC_26", "FOL_SEC_27", "FOL_SEC_28", "FOL_SEC_29", "FOL_SEC_30",
            "FOL_SCH_01", "FOL_SCH_02", "FOL_SCH_03", "FOL_SCH_04", "FOL_SCH_05", "FOL_SCH_06",
            "FOL_SIG_BORROWER",
            "FOL_SIG_LENDER"
    );

    static final Map<String, String> SECTION_TITLES = Map.ofEntries(
            Map.entry("FOL_HEADER", "Letter Header"),
            Map.entry("FOL_FACILITY_SUMMARY", "Schedule — Facility Particulars (Summary)"),
            Map.entry("FOL_SEC_01", "1. Definitions and Interpretation"),
            Map.entry("FOL_SEC_02", "2. The Facility"),
            Map.entry("FOL_SEC_03", "3. Purpose"),
            Map.entry("FOL_SEC_04", "4. Conditions of Utilisation"),
            Map.entry("FOL_SEC_05", "5. Utilisation"),
            Map.entry("FOL_SEC_06", "6. Repayment"),
            Map.entry("FOL_SEC_07", "7. Prepayment and Cancellation"),
            Map.entry("FOL_SEC_08", "8. Interest"),
            Map.entry("FOL_SEC_09", "9. Interest Periods"),
            Map.entry("FOL_SEC_10", "10. Changes to the Calculation of Interest"),
            Map.entry("FOL_SEC_11", "11. Fees"),
            Map.entry("FOL_SEC_12", "12. Tax Gross-Up and Indemnities"),
            Map.entry("FOL_SEC_13", "13. Increased Costs"),
            Map.entry("FOL_SEC_14", "14. Other Indemnities"),
            Map.entry("FOL_SEC_15", "15. Mitigation by the Lenders"),
            Map.entry("FOL_SEC_16", "16. Costs and Expenses"),
            Map.entry("FOL_SEC_17", "17. Guarantee and Indemnity"),
            Map.entry("FOL_SEC_18", "18. Representations"),
            Map.entry("FOL_SEC_19", "19. Information Undertakings"),
            Map.entry("FOL_SEC_20", "20. Financial Covenants"),
            Map.entry("FOL_SEC_21", "21. General Undertakings"),
            Map.entry("FOL_SEC_22", "22. Events of Default"),
            Map.entry("FOL_SEC_23", "23. Changes to the Lenders"),
            Map.entry("FOL_SEC_24", "24. The Agent and the Arrangers"),
            Map.entry("FOL_SEC_25", "25. Conduct of Business by the Finance Parties"),
            Map.entry("FOL_SEC_26", "26. Sharing among the Finance Parties"),
            Map.entry("FOL_SEC_27", "27. Payment Mechanics"),
            Map.entry("FOL_SEC_28", "28. Set-Off"),
            Map.entry("FOL_SEC_29", "29. Notices"),
            Map.entry("FOL_SEC_30", "30. Governing Law and Jurisdiction"),
            Map.entry("FOL_SCH_01", "Schedule 1 — Facility Particulars"),
            Map.entry("FOL_SCH_02", "Schedule 2 — Conditions Precedent"),
            Map.entry("FOL_SCH_03", "Schedule 3 — Representations"),
            Map.entry("FOL_SCH_04", "Schedule 4 — Form of Utilisation Request"),
            Map.entry("FOL_SCH_05", "Schedule 5 — Fees"),
            Map.entry("FOL_SCH_06", "Schedule 6 — Security Principles"),
            Map.entry("FOL_SIG_BORROWER", "Execution — Borrower"),
            Map.entry("FOL_SIG_LENDER", "Execution — Lenders / Agent")
    );

    @Test
    void writesWholesaleFolMasterDocxAsset() throws Exception {
        byte[] docx = buildWholesaleFolMasterDocx();
        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactlyInAnyOrderElementsOf(ANCHOR_IDS);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(document.getHeaderList()).isNotEmpty();
            assertThat(document.getFooterList()).isNotEmpty();
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            assertThat(sectPr.isSetPgMar()).isTrue();
            CTPageMar margins = sectPr.getPgMar();
            assertThat(margins.getLeft()).isNotNull();
            assertThat(sectPr.isSetPgSz()).isTrue();
            CTPageSz pageSize = sectPr.getPgSz();
            assertThat(pageSize.getW()).isEqualTo(BigInteger.valueOf(11906));
        }

        Files.createDirectories(ASSET_PATH.getParent());
        Files.write(ASSET_PATH, docx);
    }

    static byte[] buildWholesaleFolMasterDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            configurePageLayout(document);
            configureDefaultHeader(document);
            configureDefaultFooter(document);

            addCentered(document, "Meridian Global Banking Corporation", 20, true, "003366");
            addCentered(document, "Wholesale & International Banking", 11, false, "333333");
            addCentered(document, "Facility Offer Letter — Term Loan Facility", 14, true, "000000");
            addCentered(document, "(Confidential — Subject to Contract)", 10, true, "990000");
            document.createParagraph();

            for (String anchorId : ANCHOR_IDS) {
                String title = SECTION_TITLES.getOrDefault(anchorId, anchorId);
                addSection(document, title, anchorId);
            }

            document.write(output);
            return output.toByteArray();
        }
    }

    private static void configurePageLayout(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();

        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));

        CTPageMar margins = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        margins.setTop(BigInteger.valueOf(1440));
        margins.setBottom(BigInteger.valueOf(1440));
        margins.setLeft(BigInteger.valueOf(1701));
        margins.setRight(BigInteger.valueOf(1276));
        margins.setHeader(BigInteger.valueOf(708));
        margins.setFooter(BigInteger.valueOf(708));
        margins.setGutter(BigInteger.valueOf(0));
    }

    private static void configureDefaultHeader(XWPFDocument document) {
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);

        XWPFParagraph brandLine = header.createParagraph();
        brandLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun brandRun = brandLine.createRun();
        brandRun.setBold(true);
        brandRun.setFontSize(9);
        brandRun.setColor("003366");
        brandRun.setFontFamily("Calibri");
        brandRun.setText("Meridian Global Banking Corporation");

        XWPFParagraph confidentialLine = header.createParagraph();
        confidentialLine.setAlignment(ParagraphAlignment.RIGHT);
        XWPFRun confidentialRun = confidentialLine.createRun();
        confidentialRun.setBold(true);
        confidentialRun.setFontSize(7);
        confidentialRun.setColor("990000");
        confidentialRun.setFontFamily("Calibri");
        confidentialRun.setText("STRICTLY PRIVATE AND CONFIDENTIAL");

        XWPFParagraph ruleLine = header.createParagraph();
        ruleLine.setBorderBottom(org.apache.poi.xwpf.usermodel.Borders.SINGLE);
        XWPFRun ruleRun = ruleLine.createRun();
        ruleRun.setFontSize(4);
        ruleRun.setText(" ");
    }

    private static void configureDefaultFooter(XWPFDocument document) {
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);

        XWPFParagraph addressLine = footer.createParagraph();
        addressLine.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun addressRun = addressLine.createRun();
        addressRun.setFontSize(7);
        addressRun.setColor("666666");
        addressRun.setFontFamily("Calibri");
        addressRun.setText("25 Lombard Street, London EC3V 9AA  |  www.meridian-global.example  |  Regulated by the PRA & FCA");

        XWPFParagraph pageLine = footer.createParagraph();
        pageLine.setAlignment(ParagraphAlignment.CENTER);
        addPageNumberRun(pageLine);

        XWPFParagraph disclaimerLine = footer.createParagraph();
        disclaimerLine.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun disclaimerRun = disclaimerLine.createRun();
        disclaimerRun.setFontSize(7);
        disclaimerRun.setItalic(true);
        disclaimerRun.setColor("888888");
        disclaimerRun.setText("Internal demonstration document — not an offer capable of acceptance");
    }

    private static void addPageNumberRun(XWPFParagraph paragraph) {
        XWPFRun prefix = paragraph.createRun();
        prefix.setFontSize(8);
        prefix.setFontFamily("Calibri");
        prefix.setText("Page ");

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

    private static void addSection(XWPFDocument document, String title, String anchorId) {
        XWPFParagraph titleParagraph = document.createParagraph();
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(11);
        titleRun.setFontFamily("Calibri");
        titleRun.setText(title);

        XWPFParagraph filler = document.createParagraph();
        XWPFRun fillerRun = filler.createRun();
        fillerRun.setFontSize(10);
        fillerRun.setFontFamily("Calibri");
        fillerRun.setColor("444444");
        fillerRun.setText(
                "Section-level anchor in the master layout container. Long-form clause text is composed "
                        + "from approved standard modules at template authoring time."
        );

        XWPFParagraph anchorParagraph = document.createParagraph();
        XWPFRun anchorRun = anchorParagraph.createRun();
        anchorRun.setText("{{anchor:" + anchorId + "}}");
        document.createParagraph();
    }

    private static void addCentered(
            XWPFDocument document,
            String text,
            int fontSize,
            boolean bold,
            String color
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setBold(bold);
        run.setFontSize(fontSize);
        run.setFontFamily("Calibri");
        if (color != null) {
            run.setColor(color);
        }
        run.setText(text);
    }
}
