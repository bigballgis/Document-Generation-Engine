package com.bank.docgen.master.rendering;

public class DocxAnchorExtractionException extends RuntimeException {

    public DocxAnchorExtractionException(Throwable cause) {
        super("Failed to extract anchors from DOCX", cause);
    }
}
