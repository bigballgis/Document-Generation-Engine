package com.bank.docgen.rendering;

public interface PdfConversionService {

    byte[] convert(byte[] docxBytes);
}
