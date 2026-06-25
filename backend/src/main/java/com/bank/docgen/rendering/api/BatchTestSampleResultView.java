package com.bank.docgen.rendering.api;

import com.bank.docgen.rendering.domain.PreviewStatus;

public record BatchTestSampleResultView(
        String testDataSetId,
        String previewId,
        PreviewStatus status,
        int warningCount,
        int blockerCount
) {
}
