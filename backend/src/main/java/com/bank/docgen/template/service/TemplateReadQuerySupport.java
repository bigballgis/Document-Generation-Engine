package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.MasterStyleCatalogView;
import com.bank.docgen.template.api.PasteCleanRequest;
import com.bank.docgen.template.api.PasteCleanResultView;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.UUID;

/**
 * Package-private read-side binding / style / paste helpers for {@link TemplateService}.
 */
final class TemplateReadQuerySupport {

    private final TemplateAccessGuardSupport access;
    private final TemplateCurrentVersionResolver templateVersionSupport;
    private final TemplateBindingConfigurationService bindingConfigurationService;
    private final TemplateStructuredAuthoringService structuredAuthoringService;

    TemplateReadQuerySupport(
            TemplateAccessGuardSupport access,
            TemplateCurrentVersionResolver templateVersionSupport,
            TemplateBindingConfigurationService bindingConfigurationService,
            TemplateStructuredAuthoringService structuredAuthoringService
    ) {
        this.access = access;
        this.templateVersionSupport = templateVersionSupport;
        this.bindingConfigurationService = bindingConfigurationService;
        this.structuredAuthoringService = structuredAuthoringService;
    }

    BindingValidationView validateBindings(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireWritable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        return bindingConfigurationService.validateBindings(template.getMasterId(), version);
    }

    /** FOS-W7-4: compute-only for publish-gate / read-only callers. */
    BindingValidationView evaluateBindings(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireReadable(templateId, session);
        TemplateVersionEntity version = templateVersionSupport.requireInFlightDevVersion(templateId);
        return bindingConfigurationService.evaluateBindings(template.getMasterId(), version);
    }

    BindingValidationView validateBindingsForVersion(
            UUID templateId,
            TemplateVersionEntity version,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireReadable(templateId, session);
        return bindingConfigurationService.evaluateBindings(template.getMasterId(), version);
    }

    MasterStyleCatalogView getMasterStyleCatalog(UUID templateId, ManagementSessionClaims session) {
        TemplateEntity template = access.requireReadable(templateId, session);
        return structuredAuthoringService.getMasterStyleCatalog(template.getMasterId());
    }

    PasteCleanResultView pasteClean(
            UUID templateId,
            PasteCleanRequest request,
            ManagementSessionClaims session
    ) {
        access.requireWritable(templateId, session);
        return structuredAuthoringService.pasteClean(request);
    }
}
