package com.bank.docgen.rendering.goldencorpus;

/**
 * Package maturity for golden-corpus packages (K07-C4).
 */
public enum GoldenCorpusMaturity {
    ACTIVE,
    PLACEHOLDER;

    public static GoldenCorpusMaturity fromJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("manifest.maturity is required");
        }
        return GoldenCorpusMaturity.valueOf(raw.trim().toUpperCase());
    }
}
