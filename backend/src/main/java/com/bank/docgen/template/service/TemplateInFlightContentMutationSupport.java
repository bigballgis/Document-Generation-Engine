package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.event.TemplateContentChangedEvent;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Package-private in-flight content mutation bodies for TemplateService.
 */
final class TemplateInFlightContentMutationSupport {

    private final Object eventSource;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateAccessGuardSupport access;
    private final TemplateBindingConfigurationService bindingConfigurationService;
    private final ApplicationEventPublisher eventPublisher;

    TemplateInFlightContentMutationSupport(
            Object eventSource,
            TemplateCurrentVersionResolver templateVersionSupport,
            TemplateAccessGuardSupport access,
            TemplateBindingConfigurationService bindingConfigurationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.eventSource = eventSource;
        this.templateVersionSupport = templateVersionSupport;
        this.access = access;
        this.bindingConfigurationService = bindingConfigurationService;
        this.eventPublisher = eventPublisher;
    }

    VariableSchemaView upsertVariable(
            UUID templateId,
            UpsertVariableSchemaRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireWritable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        VariableSchemaView result = bindingConfigurationService.upsertVariable(version, request);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(eventSource, templateId));
        return result;
    }

    void deleteVariable(UUID templateId, String variableKey, ManagementSessionClaims session) {
        TemplateEntity template = access.requireWritable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        bindingConfigurationService.deleteVariable(version.getId(), variableKey);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(eventSource, templateId));
    }

    AnchorBindingView upsertBinding(
            UUID templateId,
            UpsertAnchorBindingRequest request,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireWritable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        AnchorBindingView result = bindingConfigurationService.upsertBinding(template.getMasterId(), version, request);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(eventSource, templateId));
        return result;
    }

    List<CompositionRuleView> saveRules(
            UUID templateId,
            List<CompositionRuleView> rules,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireWritable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireMutableInFlightDevVersion(templateId);
        access.assertDraft(template);
        List<CompositionRuleView> result = bindingConfigurationService.saveRules(version, rules);
        eventPublisher.publishEvent(new TemplateContentChangedEvent(eventSource, templateId));
        return result;
    }
}
