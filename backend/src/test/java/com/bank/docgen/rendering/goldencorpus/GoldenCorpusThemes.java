package com.bank.docgen.rendering.goldencorpus;

import java.util.List;

/**
 * CE-K07: stable theme IDs for the golden-corpus skeleton (K07-C2).
 */
public final class GoldenCorpusThemes {

    public static final String ROOT_RESOURCE = "golden-corpus";

    public static final List<String> REQUIRED_THEME_IDS = List.of(
            "dual-font-master",
            "cross-page-table",
            "nested-clauses",
            "compute-variables",
            "chinese-uppercase-amount",
            "english-locale-letter",
            "multi-currency-amount",
            "specimen-watermark",
            "encrypted-pdf",
            "long-clause-limits",
            "pdfa-2b"
    );

    private GoldenCorpusThemes() {
    }
}
