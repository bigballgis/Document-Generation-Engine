package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;

public record TemplateExportMetadataView(
        String templateId,
        String externalId,
        String groupCode,
        String name,
        String description,
        String masterId,
        TemplateLifecycleStatus lifecycleStatus,
        String releaseVersion,
        String devVersionId,
        int devVersionNumber,
        Instant exportedAt,
        String locale,
        String localeVariantFamilyId
) {
    public TemplateExportMetadataView(
            String templateId,
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId,
            TemplateLifecycleStatus lifecycleStatus,
            String releaseVersion,
            String devVersionId,
            int devVersionNumber,
            Instant exportedAt
    ) {
        this(
                templateId,
                externalId,
                groupCode,
                name,
                description,
                masterId,
                lifecycleStatus,
                releaseVersion,
                devVersionId,
                devVersionNumber,
                exportedAt,
                null,
                null
        );
    }
}
