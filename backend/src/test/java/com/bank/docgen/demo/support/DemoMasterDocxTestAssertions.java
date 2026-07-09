package com.bank.docgen.demo.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public final class DemoMasterDocxTestAssertions {

    private static final Pattern FORBIDDEN_PLACEHOLDER_PATTERN = Pattern.compile(
            "LOREM|TODO|\\{\\{placeholder|placeholder text",
            Pattern.CASE_INSENSITIVE
    );

    private DemoMasterDocxTestAssertions() {
    }

    public static void assertNoPlaceholderMarkers(byte[] docxBytes) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes))) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n'));
            document.getHeaderList().forEach(header ->
                    header.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
            document.getFooterList().forEach(footer ->
                    footer.getParagraphs().forEach(paragraph -> text.append(paragraph.getText()).append('\n')));
            String body = text.toString().toUpperCase(Locale.ROOT);
            assertThat(FORBIDDEN_PLACEHOLDER_PATTERN.matcher(body).find())
                    .as("Master DOCX must not contain LOREM/TODO/placeholder markers")
                    .isFalse();
            assertThat(body).doesNotContain("LOREM");
        }
    }
}
