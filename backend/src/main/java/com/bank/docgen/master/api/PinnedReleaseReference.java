package com.bank.docgen.master.api;

/**
 * Minimal reference to a published template version that pins a master revision
 * (CE-K01). Returned in delete-protection 409 responses so operators can locate
 * the referencing releases.
 */
public record PinnedReleaseReference(
        String templateId,
        String releaseVersion,
        String lifecycleStatus
) {
}
