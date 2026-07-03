package com.bank.docgen.rendering.service;

public class PreviewArtifactExpiredException extends RuntimeException {

    public PreviewArtifactExpiredException() {
        super("Preview artifact has expired");
    }
}
