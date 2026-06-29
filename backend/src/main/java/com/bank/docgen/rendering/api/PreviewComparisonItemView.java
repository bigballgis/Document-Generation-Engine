package com.bank.docgen.rendering.api;

import com.bank.docgen.template.domain.PreviewComparisonLocationType;
import com.bank.docgen.template.domain.PreviewComparisonSeverity;

public record PreviewComparisonItemView(
        PreviewComparisonLocationType locationType,
        String locationRef,
        PreviewComparisonSeverity severity,
        String diffCode,
        String summary
) {
}
