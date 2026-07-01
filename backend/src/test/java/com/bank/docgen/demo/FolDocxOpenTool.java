package com.bank.docgen.demo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

/** Validates generated preview DOCX opens and retains multiline anchor text. */
public final class FolDocxOpenTool {

    private FolDocxOpenTool() {
    }

    public static void main(String[] args) throws Exception {
        Path path = Path.of(args[0]);
        try (InputStream input = Files.newInputStream(path);
                XWPFDocument document = new XWPFDocument(input)) {
            int paragraphs = document.getParagraphs().size();
            int maxLen = 0;
            int multilineParagraphs = 0;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                maxLen = Math.max(maxLen, text.length());
                if (text.contains("\n")) {
                    multilineParagraphs++;
                }
            }
            System.out.println("paragraphs=" + paragraphs);
            System.out.println("maxParagraphChars=" + maxLen);
            System.out.println("multilineParagraphs=" + multilineParagraphs);
            System.out.println("containsDefinitions="
                    + document.getParagraphs().stream().anyMatch(p -> p.getText().contains("Definitions and Interpretation")));
        }
    }
}
