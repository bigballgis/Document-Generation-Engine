package com.bank.docgen.rendering.goldencorpus;

/**
 * Parsed {@code manifest.json} for one golden-corpus package.
 */
public record GoldenCorpusManifest(
        String id,
        String theme,
        GoldenCorpusMaturity maturity,
        String title,
        String renderMode,
        String pdfSource,
        boolean harnessSelfTest,
        String productPdf
) {
}
