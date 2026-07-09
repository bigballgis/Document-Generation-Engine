package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
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

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(seedDocx);
        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(replacementDocx);
        assertThat(DemoMasterDocxAssertions.readStylesXml(seedDocx)).contains("w:styleId=\"ClauseBody\"");
        assertThat(DemoMasterDocxAssertions.readFooterXml(seedDocx)).contains("NUMPAGES");

        Files.createDirectories(FIXTURES_DIR);
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(
                FIXTURES_DIR.resolve("demo-retail-letterhead-seed.docx"),
                seedDocx
        );
        com.bank.docgen.demo.support.DemoDeployAssetWriteSupport.writeBestEffort(
                FIXTURES_DIR.resolve("retail-letterhead-replacement.docx"),
                replacementDocx
        );
    }
}
