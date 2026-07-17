package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator.Evaluation;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator.Outcome;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaginationDeltaFidelitySupportTest {

    @Mock
    private DocgenRenderingProperties renderingProperties;

    @Mock
    private PdfPageCountReader pdfPageCountReader;

    private PaginationDeltaFidelitySupport support;

    @BeforeEach
    void setUp() {
        support = new PaginationDeltaFidelitySupport(renderingProperties, pdfPageCountReader);
    }

    @Test
    void evaluateUsesConfiguredBudget() {
        when(renderingProperties.getPaginationDeltaBudgetPages()).thenReturn(2);

        Evaluation evaluation = support.evaluate(6, 8);

        assertThat(evaluation.outcome()).isEqualTo(Outcome.WITHIN_BUDGET);
        assertThat(support.warningIfNeeded(evaluation)).isEmpty();
    }

    @Test
    void warningBandEmitsLowRiskPaginationDifference() {
        when(renderingProperties.getPaginationDeltaBudgetPages()).thenReturn(1);

        Evaluation evaluation = support.evaluate(6, 8);

        assertThat(evaluation.outcome()).isEqualTo(Outcome.WARNING);
        Optional<FidelityWarningView> warning = support.warningIfNeeded(evaluation);
        assertThat(warning).isPresent();
        assertThat(warning.get().code()).isEqualTo(FidelityWarningCode.LOW_RISK_PAGINATION_DIFFERENCE.name());
        assertThat(support.warningCodeIfNeeded(evaluation))
                .contains(FidelityWarningCode.LOW_RISK_PAGINATION_DIFFERENCE.name());
    }

    @Test
    void skippedWhenAuthorWordPageCountMissing() {
        when(renderingProperties.getPaginationDeltaBudgetPages()).thenReturn(1);

        Evaluation evaluation = support.evaluate(null, 6);

        assertThat(evaluation.outcome()).isEqualTo(Outcome.SKIPPED);
        assertThat(support.warningIfNeeded(evaluation)).isEmpty();
        assertThat(support.warningCodeIfNeeded(evaluation)).isEmpty();
    }

    @Test
    void measurePdfPagesDelegatesToReader() {
        byte[] pdf = new byte[] {1};
        when(pdfPageCountReader.countPages(pdf)).thenReturn(4);

        assertThat(support.measurePdfPages(pdf)).isEqualTo(4);
    }
}
