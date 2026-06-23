package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.BindingValidationStatus;

public record AnchorBindingView(
        String id,
        String anchorId,
        String declaredContentType,
        String structuredContentJson,
        BindingValidationStatus validationStatus
) {
}
