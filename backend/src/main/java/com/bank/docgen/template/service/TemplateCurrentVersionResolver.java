package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TemplateCurrentVersionResolver {

    private final TemplateVersionRepository templateVersionRepository;

    public TemplateCurrentVersionResolver(TemplateVersionRepository templateVersionRepository) {
        this.templateVersionRepository = templateVersionRepository;
    }

    public Optional<TemplateVersionEntity> findInFlightDevVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(this::isInFlight)
                .max(Comparator.comparingInt(TemplateVersionEntity::getDevVersionNumber));
    }

    public TemplateVersionEntity requireInFlightDevVersion(UUID templateId) {
        return findInFlightDevVersion(templateId).orElseThrow(TemplateNotFoundException::new);
    }

    public TemplateVersionEntity requireMutableInFlightDevVersion(UUID templateId) {
        return findInFlightDevVersion(templateId).orElseThrow(() -> new TemplateGovernanceException(
                ApiErrorCodes.TEMPLATE_VERSION_IMMUTABLE,
                "api.error.template.versionImmutable",
                HttpStatus.FORBIDDEN
        ));
    }

    public boolean hasInFlightDevVersion(UUID templateId) {
        return findInFlightDevVersion(templateId).isPresent();
    }

    public int maxDevVersionNumber(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .mapToInt(TemplateVersionEntity::getDevVersionNumber)
                .max()
                .orElse(0);
    }

    public List<TemplateVersionEntity> listVersionLinesOrdered(UUID templateId) {
        List<TemplateVersionEntity> versions = templateVersionRepository
                .findByTemplateIdOrderByDevVersionNumberDesc(templateId);
        List<TemplateVersionEntity> inFlight = versions.stream().filter(this::isInFlight).toList();
        List<TemplateVersionEntity> published = versions.stream().filter(version -> !isInFlight(version)).toList();
        return java.util.stream.Stream.concat(inFlight.stream(), published.stream()).toList();
    }

    public Optional<TemplateVersionEntity> findLatestPublishedVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> !isInFlight(version))
                .max(Comparator.comparingInt(TemplateVersionEntity::getDevVersionNumber));
    }

    public TemplateVersionEntity requireLatestVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .findFirst()
                .orElseThrow(TemplateNotFoundException::new);
    }

    public TemplateVersionEntity requireExportableVersion(UUID templateId) {
        return findInFlightDevVersion(templateId)
                .or(() -> findLatestPublishedVersion(templateId))
                .orElseThrow(TemplateNotFoundException::new);
    }

    public boolean isInFlight(TemplateVersionEntity version) {
        return version.getReleaseVersion() == null || version.getReleaseVersion().isBlank();
    }
}
