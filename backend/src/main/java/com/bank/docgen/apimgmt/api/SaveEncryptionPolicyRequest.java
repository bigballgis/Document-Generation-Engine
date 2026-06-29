package com.bank.docgen.apimgmt.api;

public record SaveEncryptionPolicyRequest(boolean docxEncryptionEnabled, boolean pdfEncryptionEnabled, boolean confirmed) {
}
