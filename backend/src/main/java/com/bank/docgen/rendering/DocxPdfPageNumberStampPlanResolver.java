package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.RenderProfile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Derives a PDF page-number stamp plan from assembled DOCX package semantics.
 */
public final class DocxPdfPageNumberStampPlanResolver {

    private static final Logger LOG = LoggerFactory.getLogger(DocxPdfPageNumberStampPlanResolver.class);

    private DocxPdfPageNumberStampPlanResolver() {
    }

    public static PdfPageNumberStampPlan resolve(byte[] docxBytes, RenderProfile renderProfile) {
        if (docxBytes == null || docxBytes.length == 0 || renderProfile == null) {
            return PdfPageNumberStampPlan.globalOnly();
        }
        try {
            String footerXml = collectFooterXml(docxBytes);
            boolean dualPageFooter = footerXml.contains("SECTIONPAGES");
            if (!dualPageFooter) {
                return PdfPageNumberStampPlan.globalOnly();
            }
            // CRCH-W0-4: true section start pages are not derivable from document.xml.
            // Fail closed to global page numbers instead of inventing one-page-per-section starts.
            LOG.warn(
                    "SECTIONPAGES footer present but section pagination unresolved; "
                            + "stamping degraded to document-global page numbers"
            );
            return PdfPageNumberStampPlan.globalOnlyWithUnresolvedSectionPagination();
        } catch (IOException ex) {
            return PdfPageNumberStampPlan.globalOnly();
        }
    }

    private static String collectFooterXml(byte[] docxBytes) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                String name = entry.getName();
                if (name.startsWith("word/footer") && name.endsWith(".xml")) {
                    builder.append(new String(zip.readAllBytes(), StandardCharsets.UTF_8));
                }
                entry = zip.getNextEntry();
            }
        }
        return builder.toString();
    }
}
