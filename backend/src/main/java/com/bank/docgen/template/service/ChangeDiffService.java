package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeDiffService {

    private final TemplateService templateService;
    private final TemplateVersionRepository templateVersionRepository;
    private final ChangeDiffDimensionSupport dimensions;

    public ChangeDiffService(
            TemplateService templateService,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository,
            AnchorBindingRepository anchorBindingRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateContentModuleReferenceRepository contentModuleReferenceRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.dimensions = new ChangeDiffDimensionSupport(
                variableSchemaRepository,
                anchorBindingRepository,
                apiPolicyRepository,
                contentModuleReferenceRepository,
                contentModuleVersionRepository,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public ChangeDiffView compute(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity candidate = requireReleaseCandidate(templateId);
        TemplateVersionEntity baseline = findLastPublishedVersion(templateId).orElse(null);
        return computeBetween(templateId, baseline, candidate);
    }

    @Transactional(readOnly = true)
    public ChangeDiffView computeForVersion(
            UUID templateId,
            TemplateVersionEntity candidate,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity baseline = findPreviousPublishedVersion(templateId, candidate).orElse(null);
        return computeBetween(templateId, baseline, candidate);
    }

    @Transactional(readOnly = true)
    public ChangeDiffView computeBetweenReleases(
            UUID templateId,
            String releaseVersionA,
            String releaseVersionB,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        if (releaseVersionA == null || releaseVersionA.isBlank()
                || releaseVersionB == null || releaseVersionB.isBlank()) {
            throw new TemplateValidationException("api.error.template.changeDiffReleaseVersionsRequired");
        }
        if (releaseVersionA.equals(releaseVersionB)) {
            TemplateVersionEntity same = requirePublishedRelease(templateId, releaseVersionA);
            return emptyDiff(templateId, same, same);
        }
        TemplateVersionEntity versionA = requirePublishedRelease(templateId, releaseVersionA);
        TemplateVersionEntity versionB = requirePublishedRelease(templateId, releaseVersionB);
        return computeBetween(templateId, versionA, versionB);
    }

    /**
     * Shared semantic diff engine entry: baseline (A) vs candidate (B).
     */
    public ChangeDiffView computeBetween(
            UUID templateId,
            TemplateVersionEntity baseline,
            TemplateVersionEntity candidate
    ) {
        Objects.requireNonNull(candidate, "candidate");
        ChangeDiffDimensionSupport.DimensionBuildResult built =
                dimensions.buildDimensions(templateId, candidate, baseline);
        List<ChangeDiffDimensionView> dimensionViews = built.dimensions();
        int totalChangeCount = dimensionViews.stream()
                .mapToInt(dimension -> dimension.added().size()
                        + dimension.removed().size()
                        + dimension.modified().size())
                .sum();
        return new ChangeDiffView(
                templateId.toString(),
                baseline == null ? null : baseline.getReleaseVersion(),
                candidate.getId().toString(),
                candidate.getReleaseVersion(),
                totalChangeCount > 0,
                totalChangeCount,
                dimensionViews,
                built.humanReadableEntries()
        );
    }

    private ChangeDiffView emptyDiff(
            UUID templateId,
            TemplateVersionEntity baseline,
            TemplateVersionEntity candidate
    ) {
        return new ChangeDiffView(
                templateId.toString(),
                baseline.getReleaseVersion(),
                candidate.getId().toString(),
                candidate.getReleaseVersion(),
                false,
                0,
                List.of(),
                List.of()
        );
    }

    private TemplateVersionEntity requirePublishedRelease(UUID templateId, String releaseVersion) {
        return templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        || (version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank()))
                .filter(version -> version.getReleaseVersion() != null && !version.getReleaseVersion().isBlank())
                .orElseThrow(TemplateNotFoundException::new);
    }

    private java.util.Optional<TemplateVersionEntity> findLastPublishedVersion(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank()
                        && version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED)
                .findFirst();
    }

    private java.util.Optional<TemplateVersionEntity> findPreviousPublishedVersion(
            UUID templateId,
            TemplateVersionEntity candidate
    ) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank()
                        && version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        && !version.getId().equals(candidate.getId())
                        && version.getDevVersionNumber() < candidate.getDevVersionNumber())
                .findFirst();
    }

    private TemplateVersionEntity requireReleaseCandidate(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getReleaseVersion() == null || version.getReleaseVersion().isBlank())
                .findFirst()
                .orElseThrow(TemplateNotFoundException::new);
    }
}
