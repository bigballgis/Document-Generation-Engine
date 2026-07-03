package com.bank.docgen.rendering;

public interface PdfConversionService {

    DocumentArtifactPipeline.PdfConversionResult convertWithResult(byte[] docxBytes, PdfConversionOptions options);

    default byte[] convert(byte[] docxBytes, PdfConversionOptions options) {
        return convertWithResult(docxBytes, options).pdfBytes();
    }

    default byte[] convert(byte[] docxBytes) {
        return convert(docxBytes, PdfConversionOptions.stampingDisabled());
    }
}
