package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.RenderProfile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Derives a PDF page-number stamp plan from assembled DOCX package semantics.
 */
public final class DocxPdfPageNumberStampPlanResolver {

    private DocxPdfPageNumberStampPlanResolver() {
    }

    public static PdfPageNumberStampPlan resolve(byte[] docxBytes, RenderProfile renderProfile) {
        if (docxBytes == null || docxBytes.length == 0 || renderProfile == null) {
            return PdfPageNumberStampPlan.globalOnly();
        }
        try {
            String documentXml = readZipEntry(docxBytes, "word/document.xml");
            String footerXml = collectFooterXml(docxBytes);
            boolean dualPageFooter = footerXml.contains("SECTIONPAGES");
            if (!dualPageFooter) {
                return PdfPageNumberStampPlan.globalOnly();
            }
            List<Integer> sectionStarts = extractSectionStartPages(documentXml);
            return PdfPageNumberStampPlan.sectionAndGlobal(sectionStarts);
        } catch (IOException ex) {
            return PdfPageNumberStampPlan.globalOnly();
        }
    }

    private static List<Integer> extractSectionStartPages(String documentXml) {
        if (documentXml == null || documentXml.isBlank()) {
            return List.of(1);
        }
        int sectionBreakCount = countOccurrences(documentXml, "<w:sectPr");
        if (sectionBreakCount <= 1) {
            return List.of(1);
        }
        List<Integer> sectionStarts = new ArrayList<>();
        sectionStarts.add(1);
        for (int sectionIndex = 1; sectionIndex < sectionBreakCount; sectionIndex++) {
            sectionStarts.add(sectionIndex + 1);
        }
        return List.copyOf(sectionStarts);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int fromIndex = 0;
        while (fromIndex >= 0) {
            fromIndex = text.indexOf(needle, fromIndex);
            if (fromIndex < 0) {
                break;
            }
            count++;
            fromIndex += needle.length();
        }
        return count;
    }

    private static String collectFooterXml(byte[] docxBytes) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.startsWith("word/footer") && name.endsWith(".xml")) {
                    builder.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
                entry = zip.getNextEntry();
            }
        }
        return builder.toString();
    }

    private static String readZipEntry(byte[] docxBytes, String entryName) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
                entry = zip.getNextEntry();
            }
        }
        throw new IOException("Missing zip entry: " + entryName);
    }
}
