package com.bank.docgen.master.rendering;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBookmark;
import org.springframework.stereotype.Component;

@Component
public class DocxAnchorExtractor {

    private static final Pattern ANCHOR_PATTERN = Pattern.compile("\\{\\{anchor:([A-Za-z0-9_.-]+)}}");
    private static final String BOOKMARK_PREFIX = "anchor.";

    public Set<String> extractAnchorIds(InputStream docxStream) {
        Set<String> anchorIds = new LinkedHashSet<>();
        try (XWPFDocument document = new XWPFDocument(docxStream)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                collectFromParagraph(paragraph, anchorIds);
            }
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            collectFromParagraph(paragraph, anchorIds);
                        }
                    }
                }
            }
            document.getHeaderList().forEach(header ->
                    header.getParagraphs().forEach(paragraph -> collectFromParagraph(paragraph, anchorIds)));
            document.getFooterList().forEach(footer ->
                    footer.getParagraphs().forEach(paragraph -> collectFromParagraph(paragraph, anchorIds)));
        } catch (IOException ex) {
            throw new DocxAnchorExtractionException(ex);
        }
        return anchorIds;
    }

    private void collectFromParagraph(XWPFParagraph paragraph, Set<String> anchorIds) {
        collectFromText(paragraph.getText(), anchorIds);
        for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
            String name = bookmark.getName();
            if (name != null && name.startsWith(BOOKMARK_PREFIX)) {
                String anchorId = name.substring(BOOKMARK_PREFIX.length());
                if (!anchorId.isBlank()) {
                    anchorIds.add(anchorId);
                }
            }
        }
    }

    private void collectFromText(String text, Set<String> anchorIds) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = ANCHOR_PATTERN.matcher(text);
        while (matcher.find()) {
            anchorIds.add(matcher.group(1));
        }
    }
}
