package com.bank.docgen.rendering;

import java.util.List;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/**
 * Package-private helper that strips master-layout filler paragraphs before assembly.
 */
final class DocxMasterLayoutFillerSupport {

    private DocxMasterLayoutFillerSupport() {
    }

    static void removeFillerParagraphs(XWPFDocument document, String fillerMarker) {
        List<IBodyElement> bodyElements = document.getBodyElements();
        for (int index = bodyElements.size() - 1; index >= 0; index--) {
            IBodyElement element = bodyElements.get(index);
            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText();
                if (text != null && text.contains(fillerMarker)) {
                    document.removeBodyElement(index);
                }
            }
        }
    }
}
