package com.bank.docgen.runtime.service;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves CE-G06 invocation fingerprint fields from the published template_version row.
 * Copies {@code master_file_hash} without re-hashing object bytes on the hot write path.
 */
@Component
public class ReleaseBundleFingerprintSupport {

    private final TemplateVersionRepository templateVersionRepository;

    public ReleaseBundleFingerprintSupport(TemplateVersionRepository templateVersionRepository) {
        this.templateVersionRepository = templateVersionRepository;
    }

    public Optional<ReleaseBundleFingerprint> resolve(UUID templateId, String resolvedReleaseVersion) {
        if (templateId == null || resolvedReleaseVersion == null || resolvedReleaseVersion.isBlank()) {
            return Optional.empty();
        }
        return templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, resolvedReleaseVersion.trim())
                .flatMap(this::toFingerprint);
    }

    private Optional<ReleaseBundleFingerprint> toFingerprint(TemplateVersionEntity version) {
        String hash = version.getMasterFileHash();
        if (hash == null || hash.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ReleaseBundleFingerprint(version.getId(), hash));
    }
}
