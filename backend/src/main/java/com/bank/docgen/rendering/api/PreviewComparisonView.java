package com.bank.docgen.rendering.api;

import java.util.List;

public record PreviewComparisonView(
        int totalDiffCount,
        int blockerCount,
        int warningCount,
        List<PreviewComparisonItemView> items
) {
}
