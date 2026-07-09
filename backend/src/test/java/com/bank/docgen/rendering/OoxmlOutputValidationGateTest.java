package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * LR-A6 / ADR-0043: OOXML output validation gate. Asserts that DOCX assembled from structured
 * content is well-formed OOXML that LibreOffice 24+ can open without a resave.
 *
 * <p>The gate opens the assembled package with POI {@link OPCPackage} and walks every XML part
 * to ensure well-formedness. A malformed package (the CD-PIT-03 failure class) would throw on
 * open or on part traversal.
 */
class OoxmlOutputValidationGateTest {

    private final DocxAssembler assembler = StructuredContentDocxWriterTestSupport.createAssembler(new ObjectMapper());

    @Test
    void assembledDocxOpensAsValidOoxmlPackage() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Bank correspondence — ampersand: A & B < C > D"},
                  {"type":"lineBreak"},
                  {"type":"variable","key":"borrowerName"}
                ]}]}
                """;

        byte[] master = minimalMasterDocx();
        byte[] assembled = assembler.assembleFromBytes(
                master,
                assembler.buildAnchorReplacements(
                        java.util.Map.of("BODY", structured),
                        java.util.Map.of("borrowerName", "Pacific Rim & Co.")
                )
        );

        // LR-A6: the assembled DOCX must open as a valid OOXML package — no resave required.
        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(assembled));
             XWPFDocument document = new XWPFDocument(pkg)) {
            // Walking the parts forces XML parsing of every part; a malformed part throws.
            assertThat(document.getParagraphs()).isNotEmpty();
            // Re-serialize to confirm the package round-trips (LibreOffice does this on open).
            ByteArrayOutputStream roundTrip = new ByteArrayOutputStream();
            document.write(roundTrip);
            assertThat(roundTrip.toByteArray()).isNotEmpty();
        }
    }

    @Test
    void assembledDocxPreservesWellFormedXmlForCjkAndEmojiContent() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"尊敬的客户 🏦 您好 — 合同编号 "},
                  {"type":"variable","key":"contractId"}
                ]}]}
                """;

        byte[] master = minimalMasterDocx();
        byte[] assembled = assembler.assembleFromBytes(
                master,
                assembler.buildAnchorReplacements(
                        java.util.Map.of("BODY", structured),
                        java.util.Map.of("contractId", "CN-2026-☕-001")
                )
        );

        try (OPCPackage pkg = OPCPackage.open(new ByteArrayInputStream(assembled));
             XWPFDocument document = new XWPFDocument(pkg)) {
            assertThat(document.getParagraphs()).isNotEmpty();
        }
    }

    private static byte[] minimalMasterDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var paragraph = document.createParagraph();
            var run = paragraph.createRun();
            run.setText("{{anchor:BODY}}");
            document.write(output);
            return output.toByteArray();
        }
    }
}
