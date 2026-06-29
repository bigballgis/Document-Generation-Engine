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
    void paste_summary_categorizesTransformedRemovedWarningBlocked() {
        String html = """
                <p>Hello</p>
                <object data="embed"></object>
                <p style="position:absolute">Floated</p>
                <script>bad()</script>
                """;

        PasteCleaningResult result = service.cleanPaste(html, "{\"nodes\":[]}");

        assertThat(result.blocked()).isTrue();
        assertThat(result.summary().transformedCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().removedCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().warningCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.summary().blockedCount()).isGreaterThanOrEqualTo(1);
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
