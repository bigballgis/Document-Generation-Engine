package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-O01-003 — PDF_A_2B selects LibreOffice PDF/A-2b export filter (not bare pdf).
 */
class LibreOfficePdfExportFiltersTest {

    @Test
    void noneUsesConventionalPdf() {
        assertThat(LibreOfficePdfExportFilters.convertToArgument(PdfArchivalProfile.NONE))
                .isEqualTo("pdf");
        assertThat(LibreOfficePdfExportFilters.convertToArgument(null))
                .isEqualTo("pdf");
    }

    @Test
    void pdfA2bUsesWriterExportSelectPdfVersion2() {
        String convertTo = LibreOfficePdfExportFilters.convertToArgument(PdfArchivalProfile.PDF_A_2B);

        assertThat(convertTo).startsWith("pdf:writer_pdf_Export:");
        assertThat(convertTo).contains("SelectPdfVersion");
        assertThat(convertTo).contains("\"value\":\"2\"");
        assertThat(convertTo).isNotEqualTo("pdf");
    }
}
