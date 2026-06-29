package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class E2eDocxFixtureGeneratorTest {

    private static final Path FIXTURES_DIR = Path.of("..", "frontend", "e2e", "fixtures");

    @Test
    void writesRetailLetterheadReplacementFixtureForE2e() throws Exception {
        byte[] seedDocx = DemoDocxFactory.buildHeaderAnchorDocx("HEADER");
        byte[] replacementDocx = DemoDocxFactory.buildRetailLetterheadReplacementDocx("HEADER");

        DocxAnchorExtractor extractor = new DocxAnchorExtractor();
        assertThat(extractor.extractAnchorIds(new ByteArrayInputStream(seedDocx))).containsExactly("HEADER");
        assertThat(extractor.extractAnchorIds(new ByteArrayInputStream(replacementDocx))).containsExactly("HEADER");

        Files.createDirectories(FIXTURES_DIR);
        Files.write(FIXTURES_DIR.resolve("demo-retail-letterhead-seed.docx"), seedDocx);
        Files.write(FIXTURES_DIR.resolve("retail-letterhead-replacement.docx"), replacementDocx);
    }
}
