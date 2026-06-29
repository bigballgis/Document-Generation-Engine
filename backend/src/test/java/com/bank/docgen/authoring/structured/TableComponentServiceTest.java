package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TableComponentServiceTest {

    private final TableComponentService service = new TableComponentService(new ObjectMapper());

    @Test
    void tableComponent_withColumnSchema_renders() {
        String json = """
                {
                  "schemaVersion": "1.0",
                  "componentKey": "TABLE-1",
                  "columnSchema": [
                    { "columnKey": "description", "widthPct": 60 },
                    { "columnKey": "amount", "widthPct": 40 }
                  ],
                  "headerRows": [
                    [
                      { "columnKey": "description", "value": "Description" },
                      { "columnKey": "amount", "value": "Amount" }
                    ]
                  ],
                  "repeatHeaderAcrossPages": true,
                  "loopRow": {
                    "loopVariable": "lineItems",
                    "cells": [
                      { "columnKey": "description", "variableKey": "itemDescription" },
                      { "columnKey": "amount", "variableKey": "itemAmount" }
                    ]
                  },
                  "footerRows": [
                    [
                      { "columnKey": "description", "value": "Total" },
                      { "columnKey": "amount", "variableKey": "totalAmount" }
                    ]
                  ]
                }
                """;

        TableComponentValidationResult result = service.validateAndBuildRenderModel(json);

        assertThat(result.fidelity().blockers()).isEmpty();
        assertThat(result.renderModel()).isPresent();
        TableComponentRenderModel model = result.renderModel().orElseThrow();
        assertThat(model.componentKey()).isEqualTo("TABLE-1");
        assertThat(model.columns()).hasSize(2);
        assertThat(model.headerRows()).hasSize(1);
        assertThat(model.loopRow()).isNotNull();
        assertThat(model.footerRows()).hasSize(1);
    }

    @Test
    void nestedTable_isBlocker() {
        String json = """
                {
                  "schemaVersion": "1.0",
                  "componentKey": "TABLE-NESTED",
                  "columnSchema": [{ "columnKey": "colA", "widthPct": 100 }],
                  "headerRows": [[{ "columnKey": "colA", "value": "Header" }]],
                  "layout": { "nestedTable": true }
                }
                """;

        TableComponentValidationResult result = service.validateAndBuildRenderModel(json);

        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.fidelity().blockers().getFirst().code()).isEqualTo(FidelityWarningCode.NESTED_TABLE);
        assertThat(result.renderModel()).isEmpty();
    }

    @Test
    void repeatHeader_acrossPages_preserved() {
        String json = """
                {
                  "schemaVersion": "1.0",
                  "componentKey": "TABLE-2",
                  "columnSchema": [{ "columnKey": "colA", "widthPct": 100 }],
                  "headerRows": [[{ "columnKey": "colA", "value": "Header" }]],
                  "repeatHeaderAcrossPages": true
                }
                """;

        TableComponentRenderModel model = service.validateAndBuildRenderModel(json).renderModel().orElseThrow();

        assertThat(model.repeatHeaderAcrossPages()).isTrue();
    }
}
