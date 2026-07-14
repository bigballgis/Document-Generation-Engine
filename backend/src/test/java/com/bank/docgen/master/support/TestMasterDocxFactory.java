package com.bank.docgen.master.support;

import com.bank.docgen.rendering.DocxMasterStyleRegistry;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyles;

/**
 * CE-K02: test masters must include {@code word/styles.xml} so upload parse fails closed only
 * for truly corrupt packages — bare POI documents omit styles by default.
 *
 * <p>Also registers the common platform style keys used by template slice tests
 * ({@code BodyText}, {@code ClauseBody}, headings, …) so styleRef validation stays green.
 */
public final class TestMasterDocxFactory {

    private static final List<String> PLATFORM_STYLE_KEYS = List.of(
            "BodyText",
            "ClauseBody",
            "Heading1",
            "Heading2",
            "Heading3",
            "ScheduleTitle",
            "TableHeader",
            "DefinedTerm",
            "SignatureBlock",
            "DisclaimerBody"
    );

    private TestMasterDocxFactory() {
    }

    public static byte[] buildWithAnchorText(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            XWPFStyles styles = document.getStyles();
            if (styles != null) {
                for (String styleKey : PLATFORM_STYLE_KEYS) {
                    DocxMasterStyleRegistry.registerBankParagraphStyle(
                            styles,
                            styleKey,
                            20,
                            DocxWordCompatibilitySupport.SYSTEM_FALLBACK_FONT,
                            styleKey.startsWith("Heading")
                                    || "ScheduleTitle".equals(styleKey)
                                    || "TableHeader".equals(styleKey)
                                    || "DefinedTerm".equals(styleKey)
                    );
                }
            }
            document.write(output);
            return output.toByteArray();
        }
    }
}
