package com.bank.docgen.apimgmt.service;

/**
 * CSV export payload for management invocation recall (CE-U11).
 */
public record ManagementInvocationCsvExport(
        String filename,
        byte[] content,
        boolean truncated,
        long totalMatched
) {
}
