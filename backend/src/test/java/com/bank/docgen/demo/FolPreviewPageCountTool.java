package com.bank.docgen.demo;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Local smoke helper: pass path to generated FOL PDF as first argument. */
public final class FolPreviewPageCountTool {

    private FolPreviewPageCountTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: FolPreviewPageCountTool <path-to.pdf>");
        }
        try (PDDocument document = Loader.loadPDF(java.nio.file.Path.of(args[0]).toFile())) {
            int pages = document.getNumberOfPages();
            String text = new PDFTextStripper().getText(document);
            boolean hasStamp = text.contains("Page 1 of " + pages);
            System.out.println("pages=" + pages);
            System.out.println("stampedFooter=" + hasStamp);
            System.out.println("containsBorrower=" + text.contains("Pacific Rim Holdings Ltd."));
            System.out.println("containsDefinitions=" + text.contains("Definitions and Interpretation"));
            if (pages < 100) {
                System.exit(2);
            }
        }
    }
}
