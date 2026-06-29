package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.ChangeDiffDimension;
import java.util.List;

public record ChangeDiffDimensionView(
        ChangeDiffDimension dimension,
        List<String> added,
        List<String> removed,
        List<ChangeDiffModificationView> modified
) {
}
