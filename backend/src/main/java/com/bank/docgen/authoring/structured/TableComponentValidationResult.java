package com.bank.docgen.authoring.structured;

import java.util.Optional;

public record TableComponentValidationResult(
        StructuredContentValidationResult fidelity,
        Optional<TableComponentRenderModel> renderModel
) {
}
