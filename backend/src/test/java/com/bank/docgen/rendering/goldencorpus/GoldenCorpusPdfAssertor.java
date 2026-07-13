package com.bank.docgen.rendering.goldencorpus;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Applies PDF text-extraction assertions (K07-C6). Never logs passwords.
 */
public final class GoldenCorpusPdfAssertor {

    public void assertPlainPdf(byte[] pdfBytes, JsonNode assertionRoot) {
        applyTextAssertions(extractText(pdfBytes, null), assertionRoot);
    }

    public void assertEncryptedPdf(byte[] encryptedPdf, String openPassword, JsonNode assertionRoot) {
        if (assertionRoot.path("requireEncrypted").asBoolean(false)) {
            assertCannotOpenWithoutPassword(encryptedPdf);
        }
        String text = extractText(encryptedPdf, openPassword);
        applyTextAssertions(text, assertionRoot);
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

    private void applyTextAssertions(String extractedText, JsonNode assertionRoot) {
        if (assertionRoot.path("deferred").asBoolean(false)) {
            return;
        }
        JsonNode assertions = assertionRoot.path("assertions");
        if (!assertions.isArray() || assertions.isEmpty()) {
            return;
        }
        String normalized = extractedText == null ? "" : extractedText;
        for (JsonNode assertion : assertions) {
            String type = assertion.path("type").asText().trim().toUpperCase(Locale.ROOT);
            String substring = assertion.path("substring").asText();
            switch (type) {
                case "TEXT_CONTAINS" -> {
                    if (!normalized.contains(substring)) {
                        throw new GoldenCorpusException(
                                "PDF TEXT_CONTAINS failed: expected substring not found in extracted text"
                        );
                    }
                }
                case "TEXT_NOT_CONTAINS" -> {
                    if (normalized.contains(substring)) {
                        throw new GoldenCorpusException(
                                "PDF TEXT_NOT_CONTAINS failed: forbidden substring was present"
                        );
                    }
                }
                default -> throw new GoldenCorpusException("Unsupported PDF assertion type: " + type);
            }
        }
    }

    private String extractText(byte[] pdfBytes, String openPassword) {
        try (PDDocument document = openPassword == null
                ? Loader.loadPDF(pdfBytes)
                : Loader.loadPDF(pdfBytes, openPassword)) {
            return new PDFTextStripper().getText(document);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed extracting PDF text", ex);
        }
    }
}
