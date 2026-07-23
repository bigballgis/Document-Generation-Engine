package com.bank.docgen.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.demo.support.DemoMasterDocxAssertions;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import java.io.ByteArrayInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * Keep-set footer layout + emphasis assembly (narrowed from Wave B expansion retail/collection demos).
 */
class DemoExpansionFooterLayoutTest {

    @Test
    void bddDemoKeep_creditLimitFooterHasRegulatedAddressWithoutFolScheduleLabel() throws Exception {
        byte[] docx = CreditLimitMasterDocxAssetGeneratorTest.buildMaster();
        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).contains("25 Lombard Street");
        assertThat(footerXml).contains("PRA");
        assertThat(footerXml).doesNotContain("Facility Offer Letter");
        assertThat(footerXml).doesNotContain("Wholesale FOL");
    }

    @Test
    void bddDemoKeep_formalDemandFooterHasMeridianAddress() throws Exception {
        byte[] docx = FormalDemandMasterDocxAssetGeneratorTest.buildMaster();
        String footerXml = DemoMasterDocxAssertions.readFooterXml(docx);
        assertThat(footerXml).containsIgnoringCase("Meridian");
        assertThat(footerXml).doesNotContain("FCA CONC");
    }

    @Test
    void bddDemoKeep_bindingSupportsBoldEmphasisRuns() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Facility amount: "},
                  {"type":"emphasis","variant":"bold","children":[{"type":"textRun","value":"GBP 25,000,000"}]}
                ]}]}
                """;
        byte[] master = CreditLimitMasterDocxAssetGeneratorTest.buildMaster();
        var assembler = StructuredContentDocxWriterTestSupport.createAssembler(
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        byte[] docx = assembler.assembleStructuredFromBytes(
                master,
                java.util.Map.of("CL_FACILITY", structured),
                java.util.Map.of(),
                java.util.Map.of()
        );
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String allText = document.getParagraphs().stream()
                    .map(p -> p.getText() == null ? "" : p.getText())
                    .reduce("", String::concat);
            assertThat(allText).contains("GBP 25,000,000");
            boolean hasBoldAmount = document.getParagraphs().stream()
                    .flatMap(p -> p.getRuns().stream())
                    .anyMatch(run -> run.isBold()
                            && run.getText(0) != null
                            && run.getText(0).contains("GBP 25,000,000"));
            assertThat(hasBoldAmount).isTrue();
        }
    }
}
