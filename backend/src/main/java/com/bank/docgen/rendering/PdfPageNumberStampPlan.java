package com.bank.docgen.rendering;

import java.util.List;

/**
 * Optional section boundaries for dual page-number stamping in PDF output.
 */
public record PdfPageNumberStampPlan(
        boolean dualPageNumbersEnabled,
        List<Integer> sectionStartPages
) {

    public static PdfPageNumberStampPlan globalOnly() {
        return new PdfPageNumberStampPlan(false, List.of(1));
    }

    public static PdfPageNumberStampPlan sectionAndGlobal(List<Integer> sectionStartPages) {
        return new PdfPageNumberStampPlan(true, sectionStartPages == null || sectionStartPages.isEmpty()
                ? List.of(1)
                : List.copyOf(sectionStartPages));
    }

    public int sectionIndexForPage(int pageNumber) {
        if (sectionStartPages == null || sectionStartPages.isEmpty()) {
            return 0;
        }
        int sectionIndex = 0;
        for (int index = 0; index < sectionStartPages.size(); index++) {
            if (sectionStartPages.get(index) <= pageNumber) {
                sectionIndex = index;
            }
        }
        return sectionIndex;
    }

    public int sectionPageNumber(int pageNumber) {
        int sectionIndex = sectionIndexForPage(pageNumber);
        int sectionStart = sectionStartPages.get(sectionIndex);
        return pageNumber - sectionStart + 1;
    }

    public int sectionPageCount(int pageNumber, int totalPages) {
        int sectionIndex = sectionIndexForPage(pageNumber);
        int sectionStart = sectionStartPages.get(sectionIndex);
        if (sectionIndex + 1 < sectionStartPages.size()) {
            return sectionStartPages.get(sectionIndex + 1) - sectionStart;
        }
        return totalPages - sectionStart + 1;
    }
}
