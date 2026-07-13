package com.bank.docgen.rendering.goldencorpus;

import java.nio.file.Path;

/**
 * One discovered golden-corpus package directory with parsed manifest.
 */
public record GoldenCorpusPackage(Path directory, GoldenCorpusManifest manifest) {

    public String id() {
        return manifest.id();
    }

    public GoldenCorpusMaturity maturity() {
        return manifest.maturity();
    }
}
