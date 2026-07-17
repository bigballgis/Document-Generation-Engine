package com.bank.docgen.rendering;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

/**
 * Counts pages in a PDF byte array (ADR-0042 measurement point). Fail-soft: returns null when
 * the PDF cannot be opened — callers must not invent a page count.
 */
@Component
public class PdfPageCountReader {

    public Integer countPages(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return null;
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pages = document.getNumberOfPages();
            return pages > 0 ? pages : null;
        } catch (IOException ex) {
            return null;
        }
    }
}
