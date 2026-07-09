package com.bank.docgen.template.service;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderContextAdapter implements TemplateRenderContextPort {

    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;

    public TemplateRenderContextAdapter(
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            TemplateContentModuleReferenceService contentModuleReferenceService
    ) {
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.contentModuleReferenceService = contentModuleReferenceService;
    }

    @Override
    public TemplateVersionEntity requireInFlightDevVersion(UUID templateId) {
        return templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
    }

    @Override
    public Map<String, String> resolvePinnedContentStructures(UUID templateVersionId) {
        return contentModuleReferenceService.resolvePinnedContentStructures(templateVersionId);
    }
}
