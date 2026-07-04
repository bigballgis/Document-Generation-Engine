package com.bank.docgen.authoring.structured;

import java.util.Optional;

public record TableComponentValidationResult(
        StructuredContentValidationResult fidelity,
        Optional<TableComponentRenderModel> renderModel
) {
    public TableComponentValidationResult {
        fidelity = StructuredContentValidationResult.of(fidelity.blockers(), fidelity.warnings());
        renderModel = renderModel.map(model -> new TableComponentRenderModel(
                model.componentKey(),
                model.columns(),
                model.headerRows(),
                model.repeatHeaderAcrossPages(),
                model.loopRow(),
                model.footerRows()
        ));
    }
}
