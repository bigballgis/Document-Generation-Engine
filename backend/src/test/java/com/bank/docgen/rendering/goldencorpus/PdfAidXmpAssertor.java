package com.bank.docgen.rendering.goldencorpus;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Lightweight PDF/A-2b identifier check via pdfaid XMP (CE-O01). Chosen over veraPDF so
 * {@code mvn verify} stays self-contained without an external veraPDF CLI/library.
 */
public final class PdfAidXmpAssertor {

    private static final Pattern PART_2 = Pattern.compile(
            "pdfaid:part\\s*=\\s*['\"]?2['\"]?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONFORMANCE_B = Pattern.compile(
            "pdfaid:conformance\\s*=\\s*['\"]?B['\"]?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PART_ELEMENT = Pattern.compile(
            "<pdfaid:part>\\s*2\\s*</pdfaid:part>",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONFORMANCE_ELEMENT = Pattern.compile(
            "<pdfaid:conformance>\\s*B\\s*</pdfaid:conformance>",
            Pattern.CASE_INSENSITIVE
    );

    private PdfAidXmpAssertor() {
    }

    public static void assertPdfA2bIdentifier(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new GoldenCorpusException("PDF/A assertion failed: empty PDF bytes");
        }
        String haystack = new String(pdfBytes, StandardCharsets.ISO_8859_1);
        String lower = haystack.toLowerCase(Locale.ROOT);
        if (!lower.contains("pdfaid")) {
            throw new GoldenCorpusException(
                    "PDF/A assertion failed: missing pdfaid XMP identifier"
            );
        }
        boolean partOk = PART_2.matcher(haystack).find() || PART_ELEMENT.matcher(haystack).find();
        boolean confOk = CONFORMANCE_B.matcher(haystack).find()
                || CONFORMANCE_ELEMENT.matcher(haystack).find();
        if (!partOk || !confOk) {
            throw new GoldenCorpusException(
                    "PDF/A assertion failed: expected pdfaid part=2 and conformance=B"
            );
        }
    }
}
