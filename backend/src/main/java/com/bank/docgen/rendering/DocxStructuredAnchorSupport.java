package com.bank.docgen.rendering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

final class DocxStructuredAnchorSupport {

    private DocxStructuredAnchorSupport() {
    }

    static void replaceInDocumentBody(
            XWPFDocument document,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            StructuredContentDocxWriter writer,
            Pattern anchorPattern
    ) {
        List<AnchorReplacement> replacements = collectStructuredAnchorReplacements(
                document.getBodyElements(),
                bindingJsonByAnchor,
                anchorPattern
        );
        for (int replacementIndex = replacements.size() - 1; replacementIndex >= 0; replacementIndex--) {
            AnchorReplacement replacement = replacements.get(replacementIndex);
            writer.replaceAnchorParagraph(
                    document,
                    replacement.paragraphIndex(),
                    replacement.structuredJson(),
                    variables,
                    pinnedModuleStructures
            );
        }
    }

    static void replaceInParagraphs(
            XWPFDocument document,
            IBody body,
            List<XWPFParagraph> paragraphs,
            Map<String, String> bindingJsonByAnchor,
            Map<String, Object> variables,
            Map<String, String> pinnedModuleStructures,
            StructuredContentDocxWriter writer,
            Pattern anchorPattern
    ) {
        List<AnchorReplacement> replacements = collectStructuredAnchorReplacements(
                paragraphs,
                bindingJsonByAnchor,
                anchorPattern
        );
        for (int replacementIndex = replacements.size() - 1; replacementIndex >= 0; replacementIndex--) {
            AnchorReplacement replacement = replacements.get(replacementIndex);
            writer.replaceStructuredAnchorInParagraph(
                    document,
                    body,
                    paragraphs.get(replacement.paragraphIndex()),
                    replacement.structuredJson(),
                    variables,
                    pinnedModuleStructures
            );
        }
    }

    static List<AnchorReplacement> collectStructuredAnchorReplacements(
            List<?> paragraphContainers,
            Map<String, String> bindingJsonByAnchor,
            Pattern anchorPattern
    ) {
        List<AnchorReplacement> replacements = new ArrayList<>();
        for (int index = 0; index < paragraphContainers.size(); index++) {
            Object container = paragraphContainers.get(index);
            XWPFParagraph paragraph;
            if (container instanceof IBodyElement bodyElement && bodyElement instanceof XWPFParagraph bodyParagraph) {
                paragraph = bodyParagraph;
            } else if (container instanceof XWPFParagraph directParagraph) {
                paragraph = directParagraph;
            } else {
                continue;
            }
            String text = paragraph.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            Matcher matcher = anchorPattern.matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String anchorId = matcher.group(1);
            String structuredJson = bindingJsonByAnchor.get(anchorId);
            if (structuredJson == null || structuredJson.isBlank()) {
                continue;
            }
            replacements.add(new AnchorReplacement(index, anchorId, structuredJson));
        }
        return replacements;
    }

    record AnchorReplacement(int paragraphIndex, String anchorId, String structuredJson) {
    }
}
