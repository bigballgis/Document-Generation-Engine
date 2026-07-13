package com.bank.docgen.authoring.structured;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Cleans pasted Word/HTML into controlled structured nodes with a safe summary (P18-T07).
 */
@Service
public class PasteCleaningService {

    public static final String MESSAGE_KEY_TRANSFORMED = "paste.summary.transformed";
    public static final String MESSAGE_KEY_REMOVED = "paste.summary.removed";
    public static final String MESSAGE_KEY_WARNING = "paste.summary.warning";
    public static final String MESSAGE_KEY_BLOCKED = "paste.summary.blocked";

    private static final Pattern PARAGRAPH_PATTERN =
            Pattern.compile("(?is)<p\\b[^>]*>(.*?)</p>");
    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("(?is)<\\s*script\\b");
    private static final Pattern IFRAME_PATTERN =
            Pattern.compile("(?is)<\\s*iframe\\b");
    private static final Pattern OBJECT_PATTERN =
            Pattern.compile("(?is)<\\s*object\\b");
    private static final Pattern ABSOLUTE_POSITION_PATTERN =
            Pattern.compile("(?is)position\\s*:\\s*absolute");
    private static final Pattern TAG_TEXT_PATTERN =
            Pattern.compile("(?s)<[^>]+>");

    private final ObjectMapper objectMapper;

    public PasteCleaningService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PasteCleaningResult cleanPaste(String sourceHtml, String prePasteStructuredContentJson) {
        String snapshot = prePasteStructuredContentJson == null ? "{}" : prePasteStructuredContentJson;
        String normalized = sourceHtml == null ? "" : sourceHtml.trim();
        List<PasteCleaningSummaryItem> summaryItems = analyzeSource(normalized);
        boolean blocked = summaryItems.stream().anyMatch(item -> item.category() == PasteCleaningCategory.BLOCKED);
        if (blocked) {
            return new PasteCleaningResult(true, null, PasteCleaningSummary.of(summaryItems), snapshot);
        }
        String cleanedJson = transformParagraphs(normalized);
        return new PasteCleaningResult(false, cleanedJson, PasteCleaningSummary.of(summaryItems), snapshot);
    }

    public String cancelToPrePaste(PasteCleaningResult result) {
        return result.prePasteSnapshotJson();
    }

    private List<PasteCleaningSummaryItem> analyzeSource(String sourceHtml) {
        List<PasteCleaningSummaryItem> items = new ArrayList<>();
        if (SCRIPT_PATTERN.matcher(sourceHtml).find() || sourceHtml.toLowerCase(Locale.ROOT).contains("javascript:")) {
            items.add(summaryItem(
                    PasteCleaningCategory.BLOCKED,
                    MESSAGE_KEY_BLOCKED,
                    "Blocked forbidden script construct in pasted HTML."
            ));
        }
        if (IFRAME_PATTERN.matcher(sourceHtml).find()) {
            items.add(summaryItem(
                    PasteCleaningCategory.BLOCKED,
                    MESSAGE_KEY_BLOCKED,
                    "Blocked iframe embed in pasted HTML."
            ));
        }
        if (OBJECT_PATTERN.matcher(sourceHtml).find()) {
            items.add(summaryItem(
                    PasteCleaningCategory.BLOCKED,
                    MESSAGE_KEY_BLOCKED,
                    "Blocked embedded object in pasted HTML."
            ));
        }
        if (ABSOLUTE_POSITION_PATTERN.matcher(sourceHtml).find()) {
            items.add(summaryItem(
                    PasteCleaningCategory.BLOCKED,
                    MESSAGE_KEY_BLOCKED,
                    "Blocked absolute positioning in pasted HTML."
            ));
        }
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(sourceHtml);
        while (paragraphMatcher.find()) {
            items.add(summaryItem(
                    PasteCleaningCategory.TRANSFORMED,
                    MESSAGE_KEY_TRANSFORMED,
                    "Transformed paragraph element into controlled structured node."
            ));
        }
        return items;
    }

    private String transformParagraphs(String sourceHtml) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", "1.0");
        ArrayNode nodes = objectMapper.createArrayNode();
        Matcher paragraphMatcher = PARAGRAPH_PATTERN.matcher(sourceHtml);
        while (paragraphMatcher.find()) {
            String innerHtml = paragraphMatcher.group(1);
            String text = stripTags(innerHtml).trim();
            if (text.isBlank()) {
                continue;
            }
            ObjectNode paragraph = objectMapper.createObjectNode();
            paragraph.put("type", "paragraph");
            ArrayNode children = objectMapper.createArrayNode();
            ObjectNode textRun = objectMapper.createObjectNode();
            textRun.put("type", "textRun");
            textRun.put("value", text);
            children.add(textRun);
            paragraph.set("children", children);
            nodes.add(paragraph);
        }
        root.set("nodes", nodes);
        return root.toString();
    }

    private String stripTags(String htmlFragment) {
        return TAG_TEXT_PATTERN.matcher(htmlFragment).replaceAll("");
    }

    private PasteCleaningSummaryItem summaryItem(
            PasteCleaningCategory category,
            String messageKey,
            String detectionSummary
    ) {
        return new PasteCleaningSummaryItem(category, messageKey, detectionSummary);
    }
}
