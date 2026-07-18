package com.bank.docgen.rendering.goldencorpus;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Applies PDF text-extraction and layout-metric assertions (K07-C6 + IBL-C1).
 * Never logs passwords. Does not perform pixel/visual compare.
 * {@code TEXT_POSITION} uses PDFBox stripper {@code XDirAdj}/{@code YDirAdj}
 * (Y measured top-down on the page).
 */
public final class GoldenCorpusPdfAssertor {

    public void assertPlainPdf(byte[] pdfBytes, JsonNode assertionRoot) {
        if (assertionRoot.path("requirePdfA2b").asBoolean(false)) {
            PdfAidXmpAssertor.assertPdfA2bIdentifier(pdfBytes);
        }
        // IBL-B3: opt-in machine gate for LIBREOFFICE (or other real) PDF/A artifacts.
        // SYNTHETIC packages keep requirePdfA2b + XMP only; use requireVeraPdf on real PDFs.
        if (assertionRoot.path("requireVeraPdf").asBoolean(false)
                && VeraPdfPdfA2bAssertor.shouldValidateOrFailIfRequired()) {
            VeraPdfPdfA2bAssertor.assertPdfA2b(pdfBytes);
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            applyAssertions(document, assertionRoot);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed opening PDF for assertions", ex);
        }
    }

    public void assertEncryptedPdf(byte[] encryptedPdf, String openPassword, JsonNode assertionRoot) {
        if (assertionRoot.path("requireEncrypted").asBoolean(false)) {
            assertCannotOpenWithoutPassword(encryptedPdf);
        }
        try (PDDocument document = Loader.loadPDF(encryptedPdf, openPassword)) {
            applyAssertions(document, assertionRoot);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed opening encrypted PDF for assertions", ex);
        }
    }

    private void assertCannotOpenWithoutPassword(byte[] encryptedPdf) {
        try {
            Loader.loadPDF(encryptedPdf).close();
            throw new GoldenCorpusException(
                    "PDF encryption assertion failed: document opened without password"
            );
        } catch (GoldenCorpusException ex) {
            throw ex;
        } catch (Exception expected) {
            // Encrypted PDFs must reject plaintext open — any exception here is success.
        }
    }

    private void applyAssertions(PDDocument document, JsonNode assertionRoot) {
        if (assertionRoot.path("deferred").asBoolean(false)) {
            return;
        }
        JsonNode assertions = assertionRoot.path("assertions");
        if (!assertions.isArray() || assertions.isEmpty()) {
            return;
        }
        String normalized = extractText(document);
        for (JsonNode assertion : assertions) {
            String type = assertion.path("type").asText().trim().toUpperCase(Locale.ROOT);
            switch (type) {
                case "TEXT_CONTAINS" -> assertTextContains(normalized, assertion.path("substring").asText());
                case "TEXT_NOT_CONTAINS" -> assertTextNotContains(normalized, assertion.path("substring").asText());
                case "PAGE_COUNT" -> assertPageCount(document, assertion);
                case "TEXT_POSITION" -> assertTextPosition(document, assertion);
                default -> throw new GoldenCorpusException("Unsupported PDF assertion type: " + type);
            }
        }
    }

    private static void assertTextContains(String normalized, String substring) {
        if (!normalized.contains(substring)) {
            throw new GoldenCorpusException(
                    "PDF TEXT_CONTAINS failed: expected substring not found in extracted text"
            );
        }
    }

    private static void assertTextNotContains(String normalized, String substring) {
        if (normalized.contains(substring)) {
            throw new GoldenCorpusException(
                    "PDF TEXT_NOT_CONTAINS failed: forbidden substring was present"
            );
        }
    }

    private static void assertPageCount(PDDocument document, JsonNode assertion) {
        int actual = document.getNumberOfPages();
        if (assertion.hasNonNull("equals")) {
            int expected = assertion.path("equals").asInt();
            if (actual != expected) {
                throw new GoldenCorpusException(
                        "PDF PAGE_COUNT failed: expected equals=" + expected + " but was " + actual
                );
            }
            return;
        }
        boolean hasMin = assertion.hasNonNull("min");
        boolean hasMax = assertion.hasNonNull("max");
        if (!hasMin && !hasMax) {
            throw new GoldenCorpusException("PDF PAGE_COUNT requires equals or min/max");
        }
        if (hasMin && actual < assertion.path("min").asInt()) {
            throw new GoldenCorpusException(
                    "PDF PAGE_COUNT failed: expected min=" + assertion.path("min").asInt()
                            + " but was " + actual
            );
        }
        if (hasMax && actual > assertion.path("max").asInt()) {
            throw new GoldenCorpusException(
                    "PDF PAGE_COUNT failed: expected max=" + assertion.path("max").asInt()
                            + " but was " + actual
            );
        }
    }

    private static void assertTextPosition(PDDocument document, JsonNode assertion) {
        String substring = assertion.path("substring").asText("");
        if (substring.isBlank()) {
            throw new GoldenCorpusException("PDF TEXT_POSITION requires non-blank substring");
        }
        int pageIndex = assertion.path("pageIndex").asInt(0);
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new GoldenCorpusException(
                    "PDF TEXT_POSITION failed: pageIndex=" + pageIndex
                            + " out of range for pageCount=" + document.getNumberOfPages()
            );
        }
        if (!assertion.hasNonNull("xMin") || !assertion.hasNonNull("xMax")
                || !assertion.hasNonNull("yMin") || !assertion.hasNonNull("yMax")) {
            throw new GoldenCorpusException("PDF TEXT_POSITION requires xMin/xMax/yMin/yMax");
        }
        float xMin = (float) assertion.path("xMin").asDouble();
        float xMax = (float) assertion.path("xMax").asDouble();
        float yMin = (float) assertion.path("yMin").asDouble();
        float yMax = (float) assertion.path("yMax").asDouble();

        TextAnchor anchor = findTextAnchor(document, pageIndex, substring);
        if (anchor == null) {
            throw new GoldenCorpusException(
                    "PDF TEXT_POSITION failed: substring not found on pageIndex=" + pageIndex
            );
        }
        if (anchor.x() < xMin || anchor.x() > xMax || anchor.y() < yMin || anchor.y() > yMax) {
            throw new GoldenCorpusException(
                    "PDF TEXT_POSITION failed: substring anchor x=" + anchor.x() + " y=" + anchor.y()
                            + " outside box xMin=" + xMin + " xMax=" + xMax
                            + " yMin=" + yMin + " yMax=" + yMax
            );
        }
    }

    private static TextAnchor findTextAnchor(PDDocument document, int pageIndex, String substring) {
        try {
            TextPositionCollector collector = new TextPositionCollector();
            collector.setStartPage(pageIndex + 1);
            collector.setEndPage(pageIndex + 1);
            collector.getText(document);
            return collector.findAnchor(substring);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed collecting PDF text positions", ex);
        }
    }

    private static String extractText(PDDocument document) {
        try {
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed extracting PDF text", ex);
        }
    }

    private record TextAnchor(float x, float y) {
    }

    private static final class TextPositionCollector extends PDFTextStripper {

        private final StringBuilder buffer = new StringBuilder();
        private final List<TextPosition> positions = new ArrayList<>();

        private TextPositionCollector() throws IOException {
            setSortByPosition(true);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition position : textPositions) {
                buffer.append(position.getUnicode());
                positions.add(position);
            }
            super.writeString(text, textPositions);
        }

        private TextAnchor findAnchor(String substring) {
            int index = buffer.indexOf(substring);
            if (index < 0 || index >= positions.size()) {
                return null;
            }
            TextPosition start = positions.get(index);
            return new TextAnchor(start.getXDirAdj(), start.getYDirAdj());
        }
    }
}
