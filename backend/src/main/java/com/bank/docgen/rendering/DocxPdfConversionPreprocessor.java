package com.bank.docgen.rendering;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

/**
 * Prepares assembled DOCX for LibreOffice PDF conversion without mutating the stored DOCX artifact.
 * Word {@code PAGE} fields in footers are removed on a conversion copy so {@link PdfPageNumberStamper}
 * is the sole page-number source in PDF output.
 */
@Component
public class DocxPdfConversionPreprocessor {

    public byte[] prepareForPdfConversion(byte[] docxBytes) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docxBytes));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (XWPFFooter footer : document.getFooterList()) {
                removePageFieldParagraphs(footer);
            }
            for (XWPFHeader header : document.getHeaderList()) {
                removePageFieldParagraphs(header);
            }
            document.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    private void removePageFieldParagraphs(XWPFFooter footer) {
        removePageFieldParagraphs(footer.getParagraphs());
    }

    private void removePageFieldParagraphs(XWPFHeader header) {
        removePageFieldParagraphs(header.getParagraphs());
    }

    private void removePageFieldParagraphs(List<XWPFParagraph> paragraphs) {
        List<XWPFParagraph> toRemove = new ArrayList<>();
        for (XWPFParagraph paragraph : paragraphs) {
            if (containsPageField(paragraph)) {
                toRemove.add(paragraph);
            }
        }
        for (XWPFParagraph paragraph : toRemove) {
            Node paragraphNode = paragraph.getCTP().getDomNode();
            Node parent = paragraphNode.getParentNode();
            if (parent != null) {
                parent.removeChild(paragraphNode);
            }
        }
    }

    boolean containsPageField(XWPFParagraph paragraph) {
        for (CTR run : paragraph.getCTP().getRList()) {
            for (CTText instruction : run.getInstrTextList()) {
                if (instruction.getStringValue() != null
                        && instruction.getStringValue().toUpperCase(Locale.ROOT).contains("PAGE")) {
                    return true;
                }
            }
        }
        return false;
    }
}
