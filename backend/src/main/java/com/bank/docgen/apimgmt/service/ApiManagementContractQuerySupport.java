package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiRoutesSummaryView;
import com.bank.docgen.apimgmt.api.ExplicitRoutePathView;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.List;
import java.util.UUID;

/**
 * Package-private caller-contract and routes-summary query helpers.
 */
final class ApiManagementContractQuerySupport {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final ContractAssemblyService contractAssemblyService;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;

    ApiManagementContractQuerySupport(
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            ContractAssemblyService contractAssemblyService,
            ApiPolicyViewMapper apiPolicyViewMapper,
            ApiManagementAccessSupport access
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.contractAssemblyService = contractAssemblyService;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.access = access;
    }

    ContractResultView getCallerContract(
            UUID templateId,
            String environment,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        RuntimeCredentialSummaryView credentialSummary = apiCredentialRepository
                .findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .filter(credential -> credential.getStatus() == ApiCredentialStatus.ACTIVE)
                .findFirst()
                .map(apiPolicyViewMapper::toRuntimeCredentialSummary)
                .orElse(null);
        return contractAssemblyService.assemble(
                template,
                policy,
                environment,
                credentialSummary,
                ContractViewAudience.ADMIN
        );
    }

    ApiRoutesSummaryView getRoutesSummary(
            UUID templateId,
            String environment,
            ManagementSessionClaims session
    ) {
        TemplateEntity template = access.requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        DefaultRouteSummaryView defaultRoute = contractAssemblyService.summarizeDefaultRoute(
                template,
                policy,
                environment
        );
        List<ExplicitRoutePathView> explicitPaths = contractAssemblyService
                .listCallableVersions(template, environment)
                .stream()
                .map(version -> new ExplicitRoutePathView(version.releaseVersion(), version.explicitVersionUrl()))
                .toList();
        return new ApiRoutesSummaryView(
                template.getExternalId(),
                policy.getDefaultRouteReleaseVersion(),
                defaultRoute.url(),
                explicitPaths
        );
    }
}
