package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxPdfPageNumberStampPlanResolverTest {

    private static final RenderProfile PROFILE = new RenderProfile(
            "rp-v1",
            "MASTER_CATALOG_LOCKED",
            "CONTROLLED_MULTILEVEL",
            "REPEAT_HEADER",
            "PROPORTIONAL_FIT",
            "SEMANTIC_FIDELITY",
            "BLOCKERS_PREVENT_PUBLISH",
            true,
            PdfArchivalProfile.NONE
    );

    @Test
    void nullOrEmptyDocx_returnsGlobalOnly() {
        assertThat(DocxPdfPageNumberStampPlanResolver.resolve(null, PROFILE).dualPageNumbersEnabled()).isFalse();
        assertThat(DocxPdfPageNumberStampPlanResolver.resolve(new byte[0], PROFILE).dualPageNumbersEnabled()).isFalse();
    }

    @Test
    void footerWithoutSectionPages_returnsGlobalOnly() throws Exception {
        byte[] docx = docxWithFooter("Page ");
        PdfPageNumberStampPlan plan = DocxPdfPageNumberStampPlanResolver.resolve(docx, PROFILE);
        assertThat(plan.dualPageNumbersEnabled()).isFalse();
        assertThat(plan.sectionPaginationUnresolved()).isFalse();
    }

    @Test
    void sectionPagesWithSingleSectPr_returnsGlobalOnly() throws Exception {
        byte[] docx = docxWithFooterAndSectPrCount(" SECTIONPAGES ", 1);
        PdfPageNumberStampPlan plan = DocxPdfPageNumberStampPlanResolver.resolve(docx, PROFILE);
        assertThat(plan.dualPageNumbersEnabled()).isFalse();
    }

    @Test
    void sectionPagesWithThreeSectPr_failsClosedToGlobalOnly_crchW04() throws Exception {
        byte[] docx = docxWithFooterAndSectPrCount(" SECTIONPAGES ", 3);
        PdfPageNumberStampPlan plan = DocxPdfPageNumberStampPlanResolver.resolve(docx, PROFILE);
        assertThat(plan.dualPageNumbersEnabled()).isFalse();
        assertThat(plan.sectionPaginationUnresolved()).isTrue();
        assertThat(plan.sectionStartPages()).containsExactly(1);
    }

    private static byte[] docxWithFooter(String footerText) throws IOException {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Body");
            document.createFooter(HeaderFooterType.DEFAULT).createParagraph().createRun().setText(footerText);
            document.write(out);
            return out.toByteArray();
        }
    }

    private static byte[] docxWithFooterAndSectPrCount(String footerSnippet, int sectPrCount) throws IOException {
        // Minimal zip: document.xml with N sectPr markers + footer1.xml containing SECTIONPAGES.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            StringBuilder body = new StringBuilder(
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                            + "<w:body><w:p><w:r><w:t>Body</w:t></w:r></w:p>"
            );
            for (int i = 0; i < sectPrCount; i++) {
                body.append("<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/></w:sectPr>");
            }
            body.append("</w:body></w:document>");
            put(zip, "word/document.xml", body.toString());
            put(zip, "word/footer1.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<w:ftr xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                            + "<w:p><w:r><w:t>" + footerSnippet + "</w:t></w:r></w:p></w:ftr>");
            put(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"></Types>");
        }
        return out.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
