package com.bank.docgen.sharedkernel.document.fidelity;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator.Evaluation;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator.Outcome;
import org.junit.jupiter.api.Test;

/**
 * BDD-PRR-C01-001…005 — ADR-0042 pagination delta thresholds (metadata-gated).
 */
class PaginationDeltaEvaluatorTest {

    @Test
    void bdd001_withinBudget_noWarningOrBlocker() {
        Evaluation atBudget = PaginationDeltaEvaluator.evaluate(6, 7, 1);
        Evaluation exact = PaginationDeltaEvaluator.evaluate(6, 6, 1);

        assertThat(atBudget.outcome()).isEqualTo(Outcome.WITHIN_BUDGET);
        assertThat(atBudget.emitsWarning()).isFalse();
        assertThat(atBudget.isPublishBlocker()).isFalse();
        assertThat(exact.outcome()).isEqualTo(Outcome.WITHIN_BUDGET);
    }

    @Test
    void bdd002_overBudgetWithin2x_emitsWarningOnly() {
        Evaluation evaluation = PaginationDeltaEvaluator.evaluate(6, 8, 1);

        assertThat(evaluation.delta()).isEqualTo(2);
        assertThat(evaluation.outcome()).isEqualTo(Outcome.WARNING);
        assertThat(evaluation.emitsWarning()).isTrue();
        assertThat(evaluation.isPublishBlocker()).isFalse();
    }

    @Test
    void bdd003_over2xBudget_publishBlocker() {
        Evaluation evaluation = PaginationDeltaEvaluator.evaluate(6, 9, 1);

        assertThat(evaluation.delta()).isEqualTo(3);
        assertThat(evaluation.outcome()).isEqualTo(Outcome.PUBLISH_BLOCKER);
        assertThat(evaluation.emitsWarning()).isTrue();
        assertThat(evaluation.isPublishBlocker()).isTrue();
    }

    @Test
    void bdd004_missingAuthorWordPageCount_skips_neverFabricates() {
        assertThat(PaginationDeltaEvaluator.evaluate(null, 6, 1).outcome()).isEqualTo(Outcome.SKIPPED);
        assertThat(PaginationDeltaEvaluator.evaluate(0, 6, 1).outcome()).isEqualTo(Outcome.SKIPPED);
        assertThat(PaginationDeltaEvaluator.evaluate(-1, 6, 1).outcome()).isEqualTo(Outcome.SKIPPED);
    }

    @Test
    void bdd005_configurableBudget_readsProvidedB() {
        // Same pages as BDD-002 (delta=2) but B=2 → within budget
        Evaluation evaluation = PaginationDeltaEvaluator.evaluate(6, 8, 2);

        assertThat(evaluation.outcome()).isEqualTo(Outcome.WITHIN_BUDGET);
        assertThat(evaluation.emitsWarning()).isFalse();
    }

    @Test
    void missingPdfPageCount_skips() {
        assertThat(PaginationDeltaEvaluator.evaluate(6, null, 1).outcome()).isEqualTo(Outcome.SKIPPED);
        assertThat(PaginationDeltaEvaluator.evaluate(6, 0, 1).outcome()).isEqualTo(Outcome.SKIPPED);
    }
}
