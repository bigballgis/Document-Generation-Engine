package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.templateVersionRepository = templateVersionRepository;
        this.dimensions = new ChangeDiffDimensionSupport(
                variableSchemaRepository,
                anchorBindingRepository,
                apiPolicyRepository,
                objectMapper
        );
    }

    @Transactional(readOnly = true)
    public ChangeDiffView compute(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity candidate = requireReleaseCandidate(templateId);
        TemplateVersionEntity baseline = findLastPublishedVersion(templateId).orElse(null);
        return buildChangeDiff(templateId, candidate, baseline);
    }

    @Transactional(readOnly = true)
    public ChangeDiffView computeForVersion(
            UUID templateId,
            TemplateVersionEntity candidate,
            ManagementSessionClaims session
    ) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity baseline = findPreviousPublishedVersion(templateId, candidate).orElse(null);
        return buildChangeDiff(templateId, candidate, baseline);
    }

    private ChangeDiffView buildChangeDiff(
            UUID templateId,
            TemplateVersionEntity candidate,
            TemplateVersionEntity baseline
    ) {
        List<ChangeDiffDimensionView> dimensionViews = dimensions.buildDimensions(templateId, candidate, baseline);
        int totalChangeCount = dimensionViews.stream()
                .mapToInt(dimension -> dimension.added().size()
                        + dimension.removed().size()
                        + dimension.modified().size())
                .sum();
        return new ChangeDiffView(
                templateId.toString(),
                baseline == null ? null : baseline.getReleaseVersion(),
                candidate.getId().toString(),
                totalChangeCount > 0,
                totalChangeCount,
                dimensionViews
        );
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
