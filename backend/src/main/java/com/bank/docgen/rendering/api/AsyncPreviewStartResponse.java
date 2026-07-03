package com.bank.docgen.rendering.api;

public record AsyncPreviewStartResponse(
        String previewId,
        String streamUrl
) {
}
