package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.StructuredContentDocxWriter;
import com.bank.docgen.rendering.StructuredContentDocxWriterTestSupport;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.sharedkernel.document.structured.DocxWriterHandledStructuredNodeTypes;
import com.bank.docgen.sharedkernel.document.structured.WriterUnsupportedStructuredNodeTypes;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A9 — writer-unsupported set must stay consistent between validation and DOCX writer reject paths.
 */
class WriterUnsupportedStructuredNodeTypesConsistencyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private NodeMatrixValidationService validationService;
    private StructuredContentDocxWriter writer;

    @BeforeEach
    void setUp() {
        validationService = new NodeMatrixValidationService(objectMapper);
        writer = StructuredContentDocxWriterTestSupport.createWriter(objectMapper);
    }

    @Test
    void authoritativeSet_isExactlyAttachmentList() {
        assertThat(WriterUnsupportedStructuredNodeTypes.jsonTypes())
                .containsExactly("attachmentListRef");
    }

    @Test
    void containsJsonType_matchesExactCamelCaseOnly() {
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("attachmentListRef")).isTrue();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("  attachmentListRef  ")).isTrue();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("qrBarcodeRef")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("attachmentlistref")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("ATTACHMENTLISTREF")).isFalse();
    }

    @Test
    void validationAndWriter_agreeOnWriterUnsupportedSet() {
        for (String jsonType : WriterUnsupportedStructuredNodeTypes.jsonTypes()) {
            String structured = "{\"nodes\":[{\"type\":\"" + jsonType + "\",\"referenceKey\":\"REF-1\"}]}";

            StructuredContentValidationResult validation = validationService.validate(structured, Set.of());
            assertThat(validation.blockers())
                    .as("validation must block %s", jsonType)
                    .isNotEmpty();
            assertThat(validation.blockers().getFirst().code())
                    .isEqualTo(FidelityWarningCode.UNSUPPORTED_NODE);

            assertThatThrownBy(() -> StructuredContentDocxWriterTestSupport.renderAnchorParagraph(
                            writer, structured, Map.of(), Map.of()))
                    .as("writer must reject %s", jsonType)
                    .isInstanceOf(DocxAssemblyException.class)
                    .extracting(ex -> ((DocxAssemblyException) ex).messageKey())
                    .isEqualTo("api.error.rendering.unsupportedNodeType");
        }
    }

    @Test
    void supportedReferenceTypes_areNotInWriterUnsupportedSet() {
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("imageRef")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("sealRef")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("qrBarcodeRef")).isFalse();
        assertThat(WriterUnsupportedStructuredNodeTypes.containsJsonType("contentModuleRef")).isFalse();
    }

    @Test
    void matrixNodeTypes_areCoveredByHandledOrWriterUnsupported() {
        // A9 strengthen: every StructuredContentNodeType must be either writer-handled or
        // explicitly writer-unsupported — no silent-omit gap for matrix-declared types.
        Set<String> covered = new HashSet<>();
        covered.addAll(DocxWriterHandledStructuredNodeTypes.jsonTypes());
        covered.addAll(WriterUnsupportedStructuredNodeTypes.jsonTypes());

        for (StructuredContentNodeType nodeType : StructuredContentNodeType.values()) {
            assertThat(covered)
                    .as("matrix type %s must be in HANDLED ∪ WriterUnsupported", nodeType.jsonType())
                    .contains(nodeType.jsonType());
        }

        assertThat(DocxWriterHandledStructuredNodeTypes.jsonTypes())
                .doesNotContainAnyElementsOf(WriterUnsupportedStructuredNodeTypes.jsonTypes());
    }
}
