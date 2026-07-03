package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

/**
 * BDD-DEMO-EXP-007 / BDD-DEMO-EXP-008 — per-demo footer layout and emphasis in body.
 */
class DemoExpansionFooterLayoutTest {

    @Test
    void bddDemoExp007_retailAccountFooterHasBranchAddressWithoutWholesaleDisclaimer() throws Exception {
        byte[] docx = RetailAccountMasterDocxAssetGeneratorTest.buildMaster(
                "Account Opening Confirmation",
                RetailAccountMasterDocxAssetGeneratorTest.OPEN_ANCHORS
        );
        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("Customer Service");
        assertThat(footerXml).contains("Manchester");
        assertThat(footerXml).doesNotContain("Wholesale");
        assertThat(footerXml).doesNotContain("Facility Offer Letter");
    }

    @Test
    void bddDemoExp008_collectionNoticeFooterHasRegulatoryDisclaimer() throws Exception {
        byte[] docx = CollectionMasterDocxAssetGeneratorTest.buildMaster(
                "Overdue Payment Collection Notice",
                CollectionMasterDocxAssetGeneratorTest.OVERDUE_ANCHORS.get(0)
        );
        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("Regulatory collection notice");
        assertThat(footerXml).contains("FCA CONC");
    }

    @Test
    void bddDemoExp008_collectionBindingSupportsBoldEmphasisRuns() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Overdue amount: "},
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"GBP 1,247.50"}]}
                ]}]}
                """;
        byte[] docx = assembleSingleAnchor("OVERDUE_COLLECTION_BODY", structured);
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String allText = document.getParagraphs().stream()
                    .map(p -> p.getText() == null ? "" : p.getText())
                    .reduce("", String::concat);
            assertThat(allText).contains("GBP 1,247.50");
            boolean hasBoldAmount = document.getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .anyMatch(run -> run.isBold() && run.getText(0).contains("GBP 1,247.50"));
            assertThat(hasBoldAmount).isTrue();
        }
    }

    private static byte[] assembleSingleAnchor(String anchorId, String structuredJson) throws Exception {
        byte[] master = CollectionMasterDocxAssetGeneratorTest.buildMaster(
                "Overdue Payment Collection Notice",
                anchorId
        );
        var assembler = new com.bank.docgen.rendering.DocxAssembler(new com.fasterxml.jackson.databind.ObjectMapper());
        return assembler.assembleStructuredFromBytes(
                master,
                java.util.Map.of(anchorId, structuredJson),
                java.util.Map.of(),
                java.util.Map.of()
        );
    }
}
