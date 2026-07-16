package com.bank.docgen.rendering.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Per-sample result for management batch-test history drill-down (CE-U18).
 * Canonical async shape plus optional legacy sync fields for FE normalize.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchTestHistorySampleResultView(
        String dataSetExternalId,
        Boolean success,
        String errorDetail,
        String docxKey,
        String pdfKey,
        String testDataSetId,
        String previewId,
        String status
) {
}
