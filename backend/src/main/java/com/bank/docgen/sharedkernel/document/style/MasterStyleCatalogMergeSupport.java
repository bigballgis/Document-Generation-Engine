package com.bank.docgen.sharedkernel.document.style;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Merges parsed master typography with platform metadata (applicableNodeTypes / renderPurpose).
 */
public final class MasterStyleCatalogMergeSupport {

    private MasterStyleCatalogMergeSupport() {
    }

    public static MasterStyleCatalog mergeWithPlatformMetadata(
            MasterStyleCatalog parsed,
            MasterStyleCatalog platformMetadata
    ) {
        if (parsed == null) {
            throw new IllegalArgumentException("parsed catalog is required");
        }
        Map<String, MasterStyleCatalogEntry> merged = new LinkedHashMap<>();
        for (MasterStyleCatalogEntry entry : parsed.stylesByKey().values()) {
            MasterStyleCatalogEntry platform = platformMetadata == null
                    ? null
                    : platformMetadata.find(entry.styleKey());
            Set<String> applicable = platform != null && !platform.applicableNodeTypes().isEmpty()
                    ? platform.applicableNodeTypes()
                    : entry.applicableNodeTypes();
            String renderPurpose = platform != null
                    && platform.renderPurpose() != null
                    && !platform.renderPurpose().isBlank()
                    ? platform.renderPurpose()
                    : entry.renderPurpose();
            MasterStyleType styleType = entry.styleType() == MasterStyleType.UNKNOWN && platform != null
                    ? inferTypeFromApplicable(platform.applicableNodeTypes())
                    : entry.styleType();
            merged.put(
                    entry.styleKey(),
                    new MasterStyleCatalogEntry(
                            entry.styleKey(),
                            applicable,
                            renderPurpose,
                            styleType,
                            entry.typography()
                    )
            );
        }
        return new MasterStyleCatalog(
                parsed.catalogVersion(),
                merged,
                parsed.docDefaults(),
                parsed.themeFonts()
        );
    }

    private static MasterStyleType inferTypeFromApplicable(Set<String> applicable) {
        if (applicable == null || applicable.isEmpty()) {
            return MasterStyleType.UNKNOWN;
        }
        if (applicable.contains("textRun") || applicable.contains("emphasis") || applicable.contains("underline")) {
            if (!applicable.contains("paragraph") && !applicable.contains("sectionHeading") && !applicable.contains("list")) {
                return MasterStyleType.CHARACTER;
            }
        }
        return MasterStyleType.PARAGRAPH;
    }
}
