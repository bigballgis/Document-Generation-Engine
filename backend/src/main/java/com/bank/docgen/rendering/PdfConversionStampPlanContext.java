package com.bank.docgen.rendering;

import java.util.function.Supplier;

/**
 * Request-scoped PDF page-number stamp plan for the active DOCX→PDF conversion.
 */
public final class PdfConversionStampPlanContext {

    private static final ThreadLocal<PdfPageNumberStampPlan> ACTIVE_PLAN = new ThreadLocal<>();

    private PdfConversionStampPlanContext() {
    }

    public static void set(PdfPageNumberStampPlan plan) {
        ACTIVE_PLAN.set(plan == null ? PdfPageNumberStampPlan.globalOnly() : plan);
    }

    public static PdfPageNumberStampPlan get() {
        PdfPageNumberStampPlan plan = ACTIVE_PLAN.get();
        return plan == null ? PdfPageNumberStampPlan.globalOnly() : plan;
    }

    public static void clear() {
        ACTIVE_PLAN.remove();
    }

    public static <T> T runWith(PdfPageNumberStampPlan plan, Supplier<T> action) {
        set(plan);
        try {
            return action.get();
        } finally {
            clear();
        }
    }
}
