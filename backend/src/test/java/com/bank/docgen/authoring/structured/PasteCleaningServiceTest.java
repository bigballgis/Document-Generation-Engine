package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PasteCleaningServiceTest {

    private final PasteCleaningService service = new PasteCleaningService(new ObjectMapper());
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void paste_simpleParagraphs_transformed() throws Exception {
        String html = "<p>Line one</p><p>Line two</p>";
        String prePaste = "{\"schemaVersion\":\"1.0\",\"nodes\":[]}";

        PasteCleaningResult result = service.cleanPaste(html, prePaste);

        assertThat(result.blocked()).isFalse();
        assertThat(result.summary().transformedCount()).isEqualTo(2);
        JsonNode cleaned = objectMapper.readTree(result.cleanedStructuredContentJson());
        assertThat(cleaned.get("nodes")).hasSize(2);
        assertThat(cleaned.get("nodes").get(0).path("type").asText()).isEqualTo("paragraph");
        assertThat(cleaned.get("nodes").get(0).path("children").get(0).path("value").asText())
                .isEqualTo("Line one");
    }

    @Test
    void paste_script_isBlocked() {
        String html = "<p>Safe</p><script>alert('x')</script>";
        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.cleanedStructuredContentJson()).isNull();
        assertThat(result.summary().blockedCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void paste_iframe_isBlocked() {
        String html = "<p>Safe</p><iframe src=\"https://example.invalid\"></iframe>";
        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.cleanedStructuredContentJson()).isNull();
        assertThat(result.summary().items())
                .anyMatch(item -> item.category() == PasteCleaningCategory.BLOCKED);
    }

    @Test
    void paste_object_isBlocked_perAdr0019() {
        // BDD-OPS-PASTE-BINDING-001 / S1 — embedded object must BLOCK (not REMOVED)
        String html = "<p>Safe</p><object data=\"embed.bin\"></object>";
        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.cleanedStructuredContentJson()).isNull();
        assertThat(result.summary().blockedCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().items())
                .filteredOn(item -> item.detectionSummary().toLowerCase().contains("object")
                        || item.detectionSummary().toLowerCase().contains("embedded"))
                .isNotEmpty()
                .allMatch(item -> item.category() == PasteCleaningCategory.BLOCKED);
        assertThat(result.summary().items())
                .noneMatch(item -> item.category() == PasteCleaningCategory.REMOVED
                        && item.detectionSummary().toLowerCase().contains("object"));
    }

    @Test
    void paste_absolutePositioning_isBlocked_perAdr0019() {
        // BDD-OPS-PASTE-BINDING-001 / S1 — absolute positioning must BLOCK (not WARNING)
        String html = "<p style=\"position:absolute; top:0\">Floated</p>";
        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.cleanedStructuredContentJson()).isNull();
        assertThat(result.summary().blockedCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().items())
                .filteredOn(item -> item.detectionSummary().toLowerCase().contains("absolute"))
                .isNotEmpty()
                .allMatch(item -> item.category() == PasteCleaningCategory.BLOCKED);
        assertThat(result.summary().items())
                .noneMatch(item -> item.category() == PasteCleaningCategory.WARNING
                        && item.detectionSummary().toLowerCase().contains("absolute"));
    }

    @Test
    void paste_objectAndAbsolute_areBlocked_notRemovedOrWarning() {
        String html = """
                <p>Hello</p>
                <object data="embed"></object>
                <p style="position:absolute">Floated</p>
                <script>bad()</script>
                """;

        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.cleanedStructuredContentJson()).isNull();
        assertThat(result.summary().transformedCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().blockedCount()).isGreaterThanOrEqualTo(3);
        assertThat(result.summary().removedCount()).isZero();
        assertThat(result.summary().warningCount()).isZero();
    }

    @Test
    void paste_summary_excludesSourcePlaintext() {
        String sensitive = "secret-customer-12345";
        String html = "<p>" + sensitive + "</p><script>run()</script>";

        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.summary().items()).isNotEmpty();
        for (PasteCleaningSummaryItem item : result.summary().items()) {
            assertThat(item.detectionSummary()).doesNotContain(sensitive);
        }
    }
}
