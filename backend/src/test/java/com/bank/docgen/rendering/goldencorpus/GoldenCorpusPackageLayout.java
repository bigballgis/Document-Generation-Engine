package com.bank.docgen.rendering.goldencorpus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Required files inside every golden-corpus package (K07-C3).
 */
public final class GoldenCorpusPackageLayout {

    public static final String MANIFEST = "manifest.json";
    public static final String INPUT_MASTER = "input/master.docx";
    public static final String INPUT_TEMPLATE = "input/template.json";
    public static final String INPUT_VARIABLES = "input/variables.json";
    public static final String EXPECTED_DOCX = "expected/docx-assertions.json";
    public static final String EXPECTED_PDF = "expected/pdf-assertions.json";

    public static final List<String> REQUIRED_RELATIVE_PATHS = List.of(
            MANIFEST,
            INPUT_MASTER,
            INPUT_TEMPLATE,
            INPUT_VARIABLES,
            EXPECTED_DOCX,
            EXPECTED_PDF
    );

    private GoldenCorpusPackageLayout() {
    }

    public static List<String> missingRequiredFiles(Path packageDir) {
        List<String> missing = new ArrayList<>();
        for (String relative : REQUIRED_RELATIVE_PATHS) {
            if (!Files.isRegularFile(packageDir.resolve(relative))) {
                missing.add(relative);
            }
        }
        return missing;
    }
}
