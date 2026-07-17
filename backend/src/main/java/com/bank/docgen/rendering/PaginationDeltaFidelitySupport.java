package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Builds fidelity warnings for ADR-0042 pagination delta (preview + runtime success paths).
 */
@Component
public class PaginationDeltaFidelitySupport {

    public static final String WARNING_MESSAGE_KEY =
            "generation.warning.fidelity.lowRiskPaginationDifference";

    private final DocgenRenderingProperties renderingProperties;
    private final PdfPageCountReader pdfPageCountReader;

    public PaginationDeltaFidelitySupport(
            DocgenRenderingProperties renderingProperties,
            PdfPageCountReader pdfPageCountReader
    ) {
        this.renderingProperties = renderingProperties;
        this.pdfPageCountReader = pdfPageCountReader;
    }

    public Integer measurePdfPages(byte[] pdfBytes) {
        return pdfPageCountReader.countPages(pdfBytes);
    }

    public PaginationDeltaEvaluator.Evaluation evaluate(Integer authorWordPageCount, Integer pdfPageCount) {
        return PaginationDeltaEvaluator.evaluate(
                authorWordPageCount,
                pdfPageCount,
                renderingProperties.getPaginationDeltaBudgetPages()
        );
    }

    public Optional<FidelityWarningView> warningIfNeeded(PaginationDeltaEvaluator.Evaluation evaluation) {
        if (!evaluation.emitsWarning()) {
            return Optional.empty();
        }
        return Optional.of(new FidelityWarningView(
                FidelityWarningCode.LOW_RISK_PAGINATION_DIFFERENCE.name(),
                WARNING_MESSAGE_KEY,
                "region=pagination",
                "pagination-delta",
                Boolean.FALSE
        ));
    }

    public Optional<String> warningCodeIfNeeded(PaginationDeltaEvaluator.Evaluation evaluation) {
        if (!evaluation.emitsWarning()) {
            return Optional.empty();
        }
        return Optional.of(FidelityWarningCode.LOW_RISK_PAGINATION_DIFFERENCE.name());
    }
}
