package com.bank.docgen.demo.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.rendering.DocxWordCompatibilitySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class DemoMasterDocxStyleSupportTest {

    @Test
    void sharedManifestDefinesRequiredBankStyleKeys() {
        MasterStyleCatalog catalog = DemoMasterDocxStyleSupport.loadSharedStyleCatalog();

        assertThat(catalog.stylesByKey().keySet())
                .containsAll(DemoMasterDocxStyleSupport.REQUIRED_BANK_STYLE_KEYS);
    }

    @Test
    void sharedManifestFileExistsUnderDeployDemoShared() {
        Path manifest = DemoMasterDocxStyleSupport.sharedManifestPath();
        assertThat(Files.exists(manifest)).isTrue();
        assertThat(Files.isRegularFile(manifest)).isTrue();
    }

    @Test
    void applySharedBankStylesEmbedsCatalogInStylesXml() throws Exception {
        byte[] docx;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);
            DocxWordCompatibilitySupport.ensureWordCompatiblePackage(document);
            document.write(output);
            docx = output.toByteArray();
        }

        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        for (String styleKey : DemoMasterDocxStyleSupport.REQUIRED_BANK_STYLE_KEYS) {
            assertThat(stylesXml)
                    .as("styles.xml must define style %s", styleKey)
                    .contains("w:styleId=\"" + styleKey + "\"");
        }
    }

    @Test
    void bankBaselineMarginsMeet254CmMinimum() throws Exception {
        byte[] docx;
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            DemoMasterDocxStyleSupport.applySharedBankStyles(document);
            document.write(output);
            docx = output.toByteArray();
        }

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            assertThat(sectPr.isSetPgMar()).isTrue();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getTop()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getBottom()).longValue()).isGreaterThanOrEqualTo(baseline);
        }
    }

    @Test
    void configureA4PageLayoutUsesManifestBaselineMargins() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            DemoMasterDocxLayoutSupport.configureA4PageLayout(document);
            CTPageMar margins = document.getDocument().getBody().getSectPr().getPgMar();
            BigInteger baseline = BigInteger.valueOf(DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS);
            assertThat(margins.getLeft()).isEqualTo(baseline);
            assertThat(margins.getRight()).isEqualTo(baseline);
        }
    }
}
