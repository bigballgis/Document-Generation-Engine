package com.bank.docgen.template.service;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;

public final class TemplateCallabilitySupport {

    private TemplateCallabilitySupport() {
    }

    public static void assertReleaseVersionCallable(
            TemplateEntity template,
            TemplateVersionEntity version,
            String resolvedVersion
    ) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            throw new TemplateValidationException("api.error.runtime.versionNotCallable");
        }
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                || version.getReleaseVersion() == null
                || version.getReleaseVersion().isBlank()
                || !resolvedVersion.equals(version.getReleaseVersion())) {
            throw new TemplateValidationException("api.error.runtime.versionNotCallable");
        }
    }
}
