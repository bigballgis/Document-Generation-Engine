package com.bank.docgen.rendering;

public class DocxAssemblyException extends RuntimeException {

    public DocxAssemblyException(Throwable cause) {
        super(cause);
    }

    public String messageKey() {
        return "api.error.rendering.generationFailed";
    }
}
