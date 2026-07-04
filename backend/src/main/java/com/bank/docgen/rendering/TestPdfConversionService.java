package com.bank.docgen.rendering;

import com.bank.docgen.template.service.TemplateValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class TestPdfConversionService implements PdfConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(TestPdfConversionService.class);

    private final PdfConversionPostProcessor pdfConversionPostProcessor;

    public TestPdfConversionService(PdfConversionPostProcessor pdfConversionPostProcessor) {
        this.pdfConversionPostProcessor = pdfConversionPostProcessor;
    }

    @Override
    public DocumentArtifactPipeline.PdfConversionResult convertWithResult(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        PdfConversionOptions resolvedOptions = options == null
                ? PdfConversionOptions.stampingDisabled()
                : options;
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("Docgen test PDF conversion");
                contentStream.endText();
            }
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                PdfPageStampResult stampResult = pdfConversionPostProcessor.finishPdf(output.toByteArray(), resolvedOptions);
                return DocumentArtifactPipeline.PdfConversionResult.of(stampResult.pdfBytes(), stampResult);
            }
        } catch (IOException ex) {
            LOG.warn("Test PDF conversion failed: {}", ex.getMessage());
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        }
    }
}
