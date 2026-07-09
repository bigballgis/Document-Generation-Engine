package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxTestAssertions;
import com.bank.docgen.demo.support.DemoMasterDocxStyleSupport;
import com.bank.docgen.master.rendering.DocxAnchorExtractor;
import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

/**
 * BDD-DEMO-TYP-019 — full-flow retail letterhead master aligns with shared P23 bank style manifest.
 */
class FullFlowMasterDocxAssetGeneratorTest {

    static final String MASTER_LAYOUT_VERSION = DemoRetailLetterheadDocxBuilder.MASTER_LAYOUT_VERSION;

    private static final String ANCHOR_ID = DemoCatalogSeeder.DEMO_ANCHOR_ID;

    @Test
    void writesFullFlowRetailLetterheadMasterWithSharedBankStyles() throws Exception {
        byte[] docx = DemoRetailLetterheadDocxBuilder.buildFullFlowMaster(ANCHOR_ID);

        assertThat(new DocxAnchorExtractor().extractAnchorIds(new ByteArrayInputStream(docx)))
                .containsExactly(ANCHOR_ID);

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(docx);
        String stylesXml = DemoMasterDocxAssertions.readStylesXml(docx);
        assertThat(stylesXml)
                .contains("w:styleId=\"ClauseBody\"")
                .contains("w:styleId=\"Heading1\"")
                .contains("w:styleId=\"SignatureBlock\"");

        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("NUMPAGES");
        assertThat(footerXml).doesNotContain("SECTIONPAGES");
        assertThat(footerXml).contains("Customer Service");
        assertThat(footerXml).contains("Manchester");

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            CTSectPr sectPr = document.getDocument().getBody().getSectPr();
            assertThat(sectPr).isNotNull();
            CTPageMar margins = sectPr.getPgMar();
            long baseline = DemoMasterDocxStyleSupport.MARGIN_BASELINE_TWIPS;
            assertThat(((BigInteger) margins.getLeft()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(((BigInteger) margins.getRight()).longValue()).isGreaterThanOrEqualTo(baseline);
            assertThat(document.getHeaderList()).isNotEmpty();
            assertThat(document.getFooterList()).isNotEmpty();
        }
    }

    @Test
    void demoDocxFactoryDelegatesToBankGradeBuilder() throws Exception {
        byte[] factoryDocx = DemoDocxFactory.buildHeaderAnchorDocx(ANCHOR_ID);
        byte[] builderDocx = DemoRetailLetterheadDocxBuilder.buildFullFlowMaster(ANCHOR_ID);

        DemoMasterDocxStyleSupport.assertSharedBankStylesPresent(factoryDocx);
        assertThat(new DocxAnchorExtractor().extractAnchorIds(new ByteArrayInputStream(factoryDocx)))
                .containsExactly(ANCHOR_ID);
        assertThat(factoryDocx.length).isGreaterThan(builderDocx.length / 2);
    }
}
