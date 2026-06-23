package com.bank.docgen.runtime.service;

import java.util.Locale;

public final class ArtifactContentTypes {

    private ArtifactContentTypes() {
    }

    public static String fromStorageKey(String storageKey) {
        if (storageKey != null && storageKey.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    }
}
