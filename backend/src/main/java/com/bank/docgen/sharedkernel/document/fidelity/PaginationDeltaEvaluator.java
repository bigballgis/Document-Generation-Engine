package com.bank.docgen.sharedkernel.document.fidelity;

/**
 * ADR-0042 / BDD-PRR-C01: metadata-gated Word↔PDF pagination delta evaluation.
 *
 * <p>Never fabricates Word page counts. When {@code authorWordPageCount} is missing or
 * non-positive, evaluation is {@link Outcome#SKIPPED} (no warning / no publish blocker).
 */
public final class PaginationDeltaEvaluator {

    private PaginationDeltaEvaluator() {
    }

    public enum Outcome {
        SKIPPED,
        WITHIN_BUDGET,
        WARNING,
        PUBLISH_BLOCKER
    }

    public record Evaluation(Outcome outcome, int delta) {

        public boolean emitsWarning() {
            return outcome == Outcome.WARNING || outcome == Outcome.PUBLISH_BLOCKER;
        }

        public boolean isPublishBlocker() {
            return outcome == Outcome.PUBLISH_BLOCKER;
        }
    }

    /**
     * @param authorWordPageCount Microsoft Word author-declared page count (not LO/PDF)
     * @param pdfPageCount measured PDF page count after successful conversion
     * @param budgetPages {@code paginationDeltaBudgetPages} (B); default Accepted = 1
     */
    public static Evaluation evaluate(
            Integer authorWordPageCount,
            Integer pdfPageCount,
            int budgetPages
    ) {
        if (authorWordPageCount == null || authorWordPageCount <= 0) {
            return new Evaluation(Outcome.SKIPPED, 0);
        }
        if (pdfPageCount == null || pdfPageCount <= 0) {
            return new Evaluation(Outcome.SKIPPED, 0);
        }
        int budget = Math.max(budgetPages, 0);
        int delta = Math.abs(pdfPageCount - authorWordPageCount);
        if (delta <= budget) {
            return new Evaluation(Outcome.WITHIN_BUDGET, delta);
        }
        if (delta <= 2 * budget) {
            return new Evaluation(Outcome.WARNING, delta);
        }
        return new Evaluation(Outcome.PUBLISH_BLOCKER, delta);
    }
}
