package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.CallableVersionView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContractAssemblyService {

    private final ContractAssemblyViewSupport views;

    public ContractAssemblyService(
            MessageResolver messageResolver,
            ObjectMapper objectMapper,
            TemplateVersionRepository templateVersionRepository,
            VariableSchemaRepository variableSchemaRepository
    ) {
        this.views = new ContractAssemblyViewSupport(
                messageResolver,
                objectMapper,
                templateVersionRepository,
                variableSchemaRepository
        );
    }

    public ContractResultView assemble(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment,
            RuntimeCredentialSummaryView credentialSummary,
            ContractViewAudience audience
    ) {
        return new ContractResultView(
                template.getExternalId(),
                views.runtimePaths(template, environment),
                views.buildDefaultRoute(template, policy, environment, audience),
                views.toPolicySummary(policy, credentialSummary, audience),
                views.buildCallableVersions(template, environment, true),
                List.of("GenerateRequest", "BatchGenerateRequest", "OutputOptions", "EncryptionOptions"),
                views.standardErrorCodes(),
                List.of()
        );
    }

    public List<CallableVersionView> listCallableVersions(TemplateEntity template, String environment) {
        return views.buildCallableVersions(template, environment, false);
    }

    public DefaultRouteSummaryView summarizeDefaultRoute(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment
    ) {
        return views.buildDefaultRoute(template, policy, environment, ContractViewAudience.ADMIN);
    }
}
