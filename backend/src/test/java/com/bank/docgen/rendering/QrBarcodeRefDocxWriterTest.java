package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CE-K06b — qrBarcodeRef DOCX writer (BDD-CE-K06b-001…005, 007, 009).
 */
class QrBarcodeRefDocxWriterTest {

    private static final String PAYLOAD = "https://pay.example/k06b";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void embedsQrBarcodeImage_andIsNoLongerUnsupported() throws Exception {
        // BDD-CE-K06b-001
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", PAYLOAD));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(countEmbeddedPictures(document)).isGreaterThanOrEqualTo(1);
            assertThat(decodeFirstPicture(document)).isEqualTo(PAYLOAD);
        }
        assertThat(com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes
                .containsJsonType("qrBarcodeRef")).isFalse();
    }

    @Test
    void usesDefaults_sizePx128_errorCorrectionM_formatQrCode() throws Exception {
        // BDD-CE-K06b-002
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", PAYLOAD));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFPicture picture = firstPicture(document);
            assertThat(pictureExtentCx(picture)).isEqualTo(Units.pixelToEMU(128));
            assertThat(pictureExtentCy(picture)).isEqualTo(Units.pixelToEMU(128));
            assertThat(decodePicture(picture)).isEqualTo(PAYLOAD);
        }
        QrBarcodeRefConfig defaults = QrBarcodeRefConfig.parse(objectMapper.readTree(
                "{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}"));
        assertThat(defaults.sizePx()).isEqualTo(128);
        assertThat(defaults.errorCorrection()).isEqualTo(QrBarcodeRefConfig.ErrorCorrection.M);
        assertThat(defaults.format()).isEqualTo(QrBarcodeRefConfig.BarcodeFormatKind.QR_CODE);
    }

    @Test
    void reflectsConfiguredSizeAndErrorCorrection() throws Exception {
        // BDD-CE-K06b-003
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR",
                  "sizePx":256,"errorCorrection":"H","format":"QR_CODE"}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", PAYLOAD));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            XWPFPicture picture = firstPicture(document);
            assertThat(pictureExtentCx(picture)).isEqualTo(Units.pixelToEMU(256));
            assertThat(decodePicture(picture)).isEqualTo(PAYLOAD);
        }
        QrBarcodeRefConfig config = QrBarcodeRefConfig.parse(objectMapper.readTree(
                "{\"sizePx\":256,\"errorCorrection\":\"H\",\"format\":\"QR_CODE\"}"));
        assertThat(config.sizePx()).isEqualTo(256);
        assertThat(config.errorCorrection()).isEqualTo(QrBarcodeRefConfig.ErrorCorrection.H);
    }

    @Test
    void failsClosedOnIllegalSize() {
        // BDD-CE-K06b-004
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR","sizePx":16}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("PAYMENT-QR", PAYLOAD)))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.qrBarcodeConfigInvalid");
    }

    @Test
    void failsClosedOnIllegalErrorCorrection() {
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR","errorCorrection":"X"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("PAYMENT-QR", PAYLOAD)))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.qrBarcodeConfigInvalid");
    }

    @Test
    void failsClosedOnUnsupportedFormat() {
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR","format":"AZTEC"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("PAYMENT-QR", PAYLOAD)))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.qrBarcodeConfigInvalid");
    }

    @Test
    void failsClosedOnMissingPayload() {
        // BDD-CE-K06b-005
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of()))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.qrBarcodePayloadMissing");
    }

    @Test
    void failsClosedOnBlankPayload() {
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]}
                """;

        assertThatThrownBy(() -> render(structured, Map.of("PAYMENT-QR", "   ")))
                .isInstanceOf(DocxAssemblyException.class)
                .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                .isEqualTo("api.error.rendering.qrBarcodePayloadMissing");
    }

    @Test
    void embedsWhenNestedInConditionBlock() throws Exception {
        // BDD-CE-K06b-007
        String structured = """
                {"nodes":[{
                  "type":"conditionBlock",
                  "conditionExpression":"${show} == true",
                  "children":[{"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}]
                }]}
                """;

        byte[] result = render(structured, Map.of("show", true, "PAYMENT-QR", PAYLOAD));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(countEmbeddedPictures(document)).isGreaterThanOrEqualTo(1);
            assertThat(decodeFirstPicture(document)).isEqualTo(PAYLOAD);
        }
    }

    @Test
    void embedsWhenInsidePinnedContentModule() throws Exception {
        String structured = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"CLAUSE-1"}]}
                """;
        Map<String, String> pinned = Map.of(
                "CLAUSE-1",
                "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}]}"
        );

        byte[] result = StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer, structured, Map.of("PAYMENT-QR", PAYLOAD), pinned);

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(countEmbeddedPictures(document)).isGreaterThanOrEqualTo(1);
            assertThat(decodeFirstPicture(document)).isEqualTo(PAYLOAD);
        }
    }

    @Test
    void embedsWhenInlineParagraphChild() throws Exception {
        String structured = """
                {"nodes":[{"type":"paragraph","children":[
                  {"type":"textRun","value":"Pay: "},
                  {"type":"qrBarcodeRef","referenceKey":"PAYMENT-QR"}
                ]}]}
                """;

        byte[] result = render(structured, Map.of("PAYMENT-QR", PAYLOAD));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(countEmbeddedPictures(document)).isGreaterThanOrEqualTo(1);
            assertThat(decodeFirstPicture(document)).isEqualTo(PAYLOAD);
        }
    }

    @Test
    void embedsCode128_andIgnoresErrorCorrection() throws Exception {
        // BDD-CE-K06b-009
        String structured = """
                {"nodes":[{"type":"qrBarcodeRef","referenceKey":"TRACK",
                  "format":"CODE_128","sizePx":128,"errorCorrection":"H"}]}
                """;
        String barcodePayload = "DOC-K06B-128";

        byte[] result = render(structured, Map.of("TRACK", barcodePayload));

        try (XWPFDocument document = StructuredContentDocxWriterTestSupport.openDocument(result)) {
            assertThat(countEmbeddedPictures(document)).isGreaterThanOrEqualTo(1);
            assertThat(decodeFirstPicture(document)).isEqualTo(barcodePayload);
        }
    }

    private byte[] render(String structuredJson, Map<String, Object> variables) throws Exception {
        return StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                writer, structuredJson, variables, Map.of());
    }

    private static long countEmbeddedPictures(XWPFDocument document) {
        return document.getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream())
                .filter(run -> !run.getEmbeddedPictures().isEmpty())
                .count();
    }

    private static XWPFPicture firstPicture(XWPFDocument document) {
        return document.getParagraphs().stream()
                .flatMap(paragraph -> paragraph.getRuns().stream())
                .flatMap(run -> run.getEmbeddedPictures().stream())
                .findFirst()
                .orElseThrow(() -> new AssertionError("No embedded picture found"));
    }

    private static String decodeFirstPicture(XWPFDocument document) throws Exception {
        return decodePicture(firstPicture(document));
    }

    private static String decodePicture(XWPFPicture picture) throws Exception {
        byte[] png = picture.getPictureData().getData();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private static long pictureExtentCx(XWPFPicture picture) {
        return picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
    }

    private static long pictureExtentCy(XWPFPicture picture) {
        return picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
    }
}
