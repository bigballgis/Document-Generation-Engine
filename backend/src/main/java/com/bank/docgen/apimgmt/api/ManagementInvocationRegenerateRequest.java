package com.bank.docgen.apimgmt.api;

/**
 * Optional body for CE-G06 controlled regenerate / PD-6 production re-issue.
 */
public record ManagementInvocationRegenerateRequest(
        String outputFormat,
        Boolean productionReissue,
        String reason
) {
    /** Convenience for default specimen regenerate (tests / callers omitting PD-6 fields). */
    public ManagementInvocationRegenerateRequest(String outputFormat) {
        this(outputFormat, null, null);
    }

    public boolean productionReissueRequested() {
        return Boolean.TRUE.equals(productionReissue);
    }
}
