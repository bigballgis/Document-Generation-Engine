package com.bank.docgen.template.service;

/**
 * CE-K01 pin-metadata JSON shape persisted on {@code template_version.pin_metadata_json}.
 *
 * <p>{@code pinOrigin} is {@code PUBLISHED} when written by the publish flow and
 * {@code PINNED_RETROACTIVELY} when written by the backfill service. The remaining
 * fields are audit metadata and are immutable after the pin is written.
 */
public record ReleaseBundlePinMetadata(
        String pinOrigin,
        String pinnedAt,
        String pinnedBy
) {
    public static final String ORIGIN_PUBLISHED = "PUBLISHED";
    public static final String ORIGIN_PINNED_RETROACTIVELY = "PINNED_RETROACTIVELY";

    public static ReleaseBundlePinMetadata published(String pinnedAt, String pinnedBy) {
        return new ReleaseBundlePinMetadata(ORIGIN_PUBLISHED, pinnedAt, pinnedBy);
    }

    public static ReleaseBundlePinMetadata retroactive(String pinnedAt, String pinnedBy) {
        return new ReleaseBundlePinMetadata(ORIGIN_PINNED_RETROACTIVELY, pinnedAt, pinnedBy);
    }
}
