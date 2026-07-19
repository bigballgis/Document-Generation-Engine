package com.bank.docgen.template.service;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderContextAdapter implements TemplateRenderContextPort {

    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final CompositionInclusionRuleService compositionInclusionRuleService;
    private final TemplateVersionRepository templateVersionRepository;

    public TemplateRenderContextAdapter(
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CompositionInclusionRuleService compositionInclusionRuleService,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.compositionInclusionRuleService = compositionInclusionRuleService;
        this.templateVersionRepository = templateVersionRepository;
    }

    @Override
    public TemplateVersionEntity requireInFlightDevVersion(UUID templateId) {
        return templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
    }

    @Override
    public Map<String, String> resolvePinnedContentStructures(
            UUID templateVersionId,
            CompositionInclusionAxes inclusionAxes
    ) {
        Map<String, String> allPinned =
                contentModuleReferenceService.resolvePinnedContentStructures(templateVersionId);
        TemplateVersionEntity version = templateVersionRepository.findById(templateVersionId).orElse(null);
        if (version == null) {
            return allPinned;
        }
        return CompositionInclusionAssemblySupport.apply(
                version,
                allPinned,
                contentModuleReferenceService.resolvePinnedJurisdictions(templateVersionId),
                compositionInclusionRuleService.loadRules(version),
                inclusionAxes == null ? CompositionInclusionAxes.empty() : inclusionAxes
        ).pinnedStructures();
    }
}
