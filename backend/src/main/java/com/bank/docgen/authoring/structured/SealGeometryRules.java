package com.bank.docgen.authoring.structured;

/**
 * Confirmed IBL-B5 seal geometry contract (pt, page-local AABB, closed containment).
 *
 * <p>SoT: {@code docs/behavior/ibl-b5-seal-geometry.md} §4.
 */
public final class SealGeometryRules {

    public static final String UNITS = "pt";
    public static final double DEFAULT_SEAL_WIDTH_PT = 48.0d;
    public static final double DEFAULT_SEAL_HEIGHT_PT = 48.0d;

    private SealGeometryRules() {
    }

    /**
     * Closed-edge full footprint containment on the same pageIndex.
     */
    public static boolean fullyContains(SealAxisAlignedBox area, SealAxisAlignedBox seal) {
        if (area == null || seal == null) {
            return false;
        }
        return seal.pageIndex() == area.pageIndex()
                && seal.xPt() >= area.xPt()
                && seal.yPt() >= area.yPt()
                && seal.xPt() + seal.widthPt() <= area.xPt() + area.widthPt()
                && seal.yPt() + seal.heightPt() <= area.yPt() + area.heightPt();
    }

    public record SealAxisAlignedBox(
            int pageIndex,
            double xPt,
            double yPt,
            double widthPt,
            double heightPt
    ) {
    }
}
