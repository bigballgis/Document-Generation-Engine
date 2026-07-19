package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.api.CompositionInclusionRulesResultView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.event.TemplateContentChangedEvent;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompositionInclusionRuleService {

    private final TemplateService templateService;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Object eventSource = new Object();

    public CompositionInclusionRuleService(
            TemplateService templateService,
            TemplateCurrentVersionResolver templateVersionSupport,
            TemplateVersionRepository templateVersionRepository,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.templateService = templateService;
        this.templateVersionSupport = templateVersionSupport;
        this.templateVersionRepository = templateVersionRepository;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public CompositionInclusionRulesResultView getRules(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        return new CompositionInclusionRulesResultView(loadRules(version));
    }

    @Transactional
    public CompositionInclusionRulesResultView putRules(
            UUID templateId,
            List<CompositionInclusionRuleView> rules,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = templateService.requireWritableTemplate(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        assertDraft(template);
        CompositionInclusionRuleValidator.validate(
                rules,
                contentModuleReferenceService.listReferenceKeys(version.getId())
        );
        List<CompositionInclusionRuleView> normalized = normalize(rules);
        try {
            version.setCompositionInclusionRulesJson(objectMapper.writeValueAsString(normalized));
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidRulesJson");
        }
        templateVersionRepository.save(version);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(eventSource, templateId));
        return new CompositionInclusionRulesResultView(loadRules(version));
    }

    @Transactional
    public void replaceRulesForImport(
            UUID templateId,
            List<CompositionInclusionRuleView> rules,
            ManagementSessionClaims session
    ) {
        putRules(templateId, rules == null ? List.of() : rules, session);
    }

    public List<CompositionInclusionRuleView> loadRules(TemplateVersionEntity version) {
        if (version == null
                || version.getCompositionInclusionRulesJson() == null
                || version.getCompositionInclusionRulesJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    version.getCompositionInclusionRulesJson(),
                    new TypeReference<List<CompositionInclusionRuleView>>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.invalidRulesJson");
        }
    }

    private List<CompositionInclusionRuleView> normalize(List<CompositionInclusionRuleView> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        return rules.stream()
                .map(rule -> new CompositionInclusionRuleView(
                        rule.ruleId().trim(),
                        rule.referenceKey().trim(),
                        new CompositionInclusionMatchView(
                                blankToNull(rule.match().jurisdiction()),
                                blankToNull(rule.match().product()),
                                blankToNull(rule.match().channel())
                        ),
                        Integer.valueOf(rule.resolvedPriority()),
                        Boolean.valueOf(rule.resolvedRequiredInclusion())
                ))
                .toList();
    }

    private static void assertDraft(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.DRAFT) {
            throw new TemplateValidationException("api.error.template.invalidState");
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
