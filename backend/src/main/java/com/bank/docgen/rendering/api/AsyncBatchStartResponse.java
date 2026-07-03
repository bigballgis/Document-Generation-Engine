package com.bank.docgen.rendering.api;

public record AsyncBatchStartResponse(
        String runId,
        String streamUrl
) {
}
