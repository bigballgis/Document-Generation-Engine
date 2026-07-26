package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.util.Units;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * POI fidelity regression safety net for {@link StructuredContentDocxWriter}.
 * BDD: BDD-F1-A1-004 — must stay green before F1-T03 dual-track removal.
 */
/**
 * Peeled from StructuredContentDocxWriterTest (AI-SCALE #169).
 */
class StructuredContentDocxWriterModuleMediaTest extends StructuredContentDocxWriterTestFixtures {

    @Test
    void expandsContentModuleRefFromPinnedStructure() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"textRun\",\"value\":\"Locked clause\"}]}]}"
        );

        byte[] result = render(structured, Map.of(), pinned);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(document.getParagraphs().getFirst().getText()).isEqualTo("Locked clause");
        }
    }
    @Test
    void failsClosedWhenPinnedStructureMissing() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MISSING"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of(), Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.messageKey())
                            .isEqualTo("api.error.validation.contentModuleStructureMissing");
                    assertThat(assemblyException.errorCode()).isEqualTo("CONTENT_MODULE_STRUCTURE_MISSING");
                    assertThat(assemblyException.category()).isEqualTo("VALIDATION");
                });
    }
    @Test
    void failsClosedWhenContentModuleNestingCycleDetected() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"A"}]}
                """;
        Map<String, String> pinned = Map.of(
                "A",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"B\"}]}",
                "B",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"A\"}]}"
        );

        assertThatThrownBy(() -> render(structured, Map.of(), pinned))
                .isInstanceOf(DocxAssemblyException.class)
                .satisfies(ex -> {
                    DocxAssemblyException assemblyException = (DocxAssemblyException) ex;
                    assertThat(assemblyException.errorCode()).isEqualTo("CONTENT_MODULE_NESTING_CYCLE");
                    assertThat(assemblyException.messageKey())
                            .isEqualTo("api.error.validation.contentModuleNestingCycle");
                });
    }
    @Test
    void failsClosedWhenPinnedStructureBlank() {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of(), Map.of("CLAUSE-1", "   ")))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.validation.contentModuleStructureMissing");
    }
    @Test
    void embedsImageAndSealReferences() throws Exception {
        String structured = """
                {"nodes":[
                  {"type":"paragraph","children":[{"type":"imageRef","imageRef":"IMG-1"}]},
                  {"type":"paragraph","children":[{"type":"sealRef","referenceKey":"SEAL-1"}]}
                ]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(2);
        }
    }
    @Test
    void embedsQrBarcodeNode() throws Exception {
        // CE-K06b — success path replaces former unsupported fail-closed for qrBarcodeRef
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }
    @Test
    void writesAttachmentListNodeAsNumberedParagraphs() throws Exception {
        // CE-K06c — success path replaces former unsupported fail-closed for attachmentListRef
        String structured = """
                {"nodes":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]}
                """;

        byte[] result = render(structured, Map.of(
                "ATTACHMENTS", List.of("Annex A", "Annex B")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long numbered = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null
                            && paragraph.getCTP().getPPr().getNumPr() != null)
                    .count();
            assertThat(numbered).isEqualTo(2);
        }
    }
    @Test
    void embedsQrBarcodeNestedInConditionBlock() throws Exception {
        String structured = """
                {"nodes":[{
                  "type":"conditionBlock",
                  "conditionExpression":"${show} == true",
                  "children":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]
                }]}
                """;

        byte[] result = render(structured, Map.of("show", true, "PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }
    @Test
    void writesAttachmentListNestedInLoopBlock() throws Exception {
        // CE-K06c — nested success path under loopBlock
        String structured = """
                {"nodes":[{
                  "type":"loopBlock",
                  "loopVariable":"items",
                  "children":[{"type":"attachmentListRef","referenceKey":"ATTACHMENTS"}]
                }]}
                """;

        byte[] result = render(structured, Map.of(
                "items", List.of(Map.of("n", "1")),
                "ATTACHMENTS", List.of("Loop annex")
        ));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long numbered = document.getParagraphs().stream()
                    .filter(paragraph -> paragraph.getCTP().getPPr() != null
                            && paragraph.getCTP().getPPr().getNumPr() != null)
                    .count();
            assertThat(numbered).isGreaterThanOrEqualTo(1);
        }
    }
    @Test
    void embedsQrBarcodeInsidePinnedContentModule() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}]}"
        );

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"), pinned);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }
    @Test
    void embedsQrBarcodeAsParagraphInlineChild() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Pay: "},
                  {"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", "https://pay.example/k06b"));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            long pictureCount = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .filter(run -> !run.getEmbeddedPictures().isEmpty())
                    .count();
            assertThat(pictureCount).isGreaterThanOrEqualTo(1);
        }
    }
    @Test
    void preservesImageAspectRatioInside48ptBox_crchW02() throws Exception {
        StructuredContentImageResolver resolver = keyAgnosticResolver(pngBytes(200, 100));
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper, resolver);
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"imageRef","imageRef":"IMG-WIDE"}]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFPicture picture = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .flatMap(run -> run.getEmbeddedPictures().stream())
                    .findFirst()
                    .orElseThrow();
            long cx = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
            long cy = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
            assertThat(cx).isEqualTo(Units.toEMU(48));
            assertThat(cy).isEqualTo(Units.toEMU(24));
        }
    }
    @Test
    void rendersSealAtDeclaredSealBoxSize_crchW03() throws Exception {
        StructuredContentImageResolver resolver = keyAgnosticResolver(pngBytes(64, 64));
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper, resolver);
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{
                  "type":"sealRef",
                  "referenceKey":"OFFICIAL_SEAL",
                  "placement":{
                    "authorizedAreaId":"AREA_1",
                    "sealBox":{"pageIndex":0,"xPt":100,"yPt":100,"widthPt":120,"heightPt":90}
                  }
                }]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFPicture picture = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .flatMap(run -> run.getEmbeddedPictures().stream())
                    .findFirst()
                    .orElseThrow();
            long cx = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
            long cy = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
            assertThat(cx).isEqualTo(Units.toEMU(120));
            assertThat(cy).isEqualTo(Units.toEMU(90));
        }
    }
    @Test
    void rendersSealAtDefaultSizeWhenPlacementAbsent_crchW03() throws Exception {
        StructuredContentImageResolver resolver = keyAgnosticResolver(pngBytes(64, 64));
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper, resolver);
        String structured = """
                {"nodes":[{"type":"paragraph","children":[{"type":"sealRef","referenceKey":"OFFICIAL_SEAL"}]}]}
                """;

        byte[] result = render(structured, Map.of());

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFPicture picture = document.getParagraphs().stream()
                    .flatMap(paragraph -> paragraph.getRuns().stream())
                    .flatMap(run -> run.getEmbeddedPictures().stream())
                    .findFirst()
                    .orElseThrow();
            long cx = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
            long cy = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
            assertThat(cx).isEqualTo(Units.toEMU(48));
            assertThat(cy).isEqualTo(Units.toEMU(48));
        }
    }
}
