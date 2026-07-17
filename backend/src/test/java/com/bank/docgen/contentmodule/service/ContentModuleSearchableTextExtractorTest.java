package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentModuleSearchableTextExtractorTest {

    private ContentModuleSearchableTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ContentModuleSearchableTextExtractor(new ObjectMapper());
    }

    @Test
    void extractsParagraphAndTextRunLeaves_skipsTypeEnums() {
        String json = """
                {
                  "blocks": [
                    {
                      "type": "paragraph",
                      "runs": [
                        { "type": "textRun", "text": "force majeure carve-out-xyz" }
                      ]
                    },
                    {
                      "type": "list",
                      "items": [
                        { "text": "list item one" }
                      ]
                    }
                  ]
                }
                """;
        String text = extractor.extract(json);
        assertThat(text).contains("force majeure carve-out-xyz");
        assertThat(text).contains("list item one");
        assertThat(text).doesNotContain("paragraph");
        assertThat(text).doesNotContain("textRun");
    }

    @Test
    void extractFailure_returnsEmpty() {
        assertThat(extractor.extract("{not-json")).isEmpty();
        assertThat(extractor.extract(null)).isEmpty();
    }
}
