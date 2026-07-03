package com.bank.docgen.apimgmt.api;

public record SaveInvocationRetentionRequest(
        boolean saveGeneratedDocuments,
        int invocationRecordRetentionDays,
        int documentRetentionDays,
        boolean confirmed
) {
}
