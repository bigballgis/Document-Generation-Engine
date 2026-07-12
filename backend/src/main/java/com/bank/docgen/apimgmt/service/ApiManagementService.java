package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiCredentialCreatedView;
import com.bank.docgen.apimgmt.api.ApiCredentialSummaryView;
import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.api.ApiRoutesSummaryView;
import com.bank.docgen.apimgmt.api.ExplicitRoutePathView;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapper;
import com.bank.docgen.apimgmt.api.RotateCredentialResponse;
import com.bank.docgen.apimgmt.api.SaveAdGroupsRequest;
import com.bank.docgen.apimgmt.api.SaveBatchLimitsRequest;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.SaveEncryptionPolicyRequest;
import com.bank.docgen.apimgmt.api.SaveInvocationRetentionRequest;
import com.bank.docgen.apimgmt.api.SaveOutputPolicyRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.domain.ApiCredentialStatus;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.DefaultRouteSummaryView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.runtime.api.RuntimeCredentialSummaryView;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiManagementService {

    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final GroupAccessService groupAccessService;
    private final ContractAssemblyService contractAssemblyService;
    private final ApiPolicyViewMapper apiPolicyViewMapper;
    private final ApiManagementAccessSupport access;
    private final ApiCredentialCommandSupport credentials;
    private final ApiPolicyCommandSupport policies;

    public ApiManagementService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            GroupAccessService groupAccessService,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder managementAuditRecorder,
            ContractAssemblyService contractAssemblyService,
            ObjectMapper objectMapper,
            ApiPolicyVersionSnapshotService apiPolicyVersionSnapshotService,
            TemplateVersionRepository templateVersionRepository,
            TemplateAdGroupAuthorizationCache templateAdGroupAuthorizationCache,
            ApiPolicyImpactPreviewService apiPolicyImpactPreviewService,
            ApiPolicyViewMapper apiPolicyViewMapper
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.groupAccessService = groupAccessService;
        this.contractAssemblyService = contractAssemblyService;
        this.apiPolicyViewMapper = apiPolicyViewMapper;
        this.access = new ApiManagementAccessSupport(
                templateService,
                apiPolicyRepository,
                this.groupAccessService
        );
        this.credentials = new ApiCredentialCommandSupport(
                apiPolicyRepository,
                apiCredentialRepository,
                passwordHashService,
                managementAuditRecorder,
                apiPolicyViewMapper,
                access
        );
        this.policies = new ApiPolicyCommandSupport(
                apiPolicyRepository,
                managementAuditRecorder,
                objectMapper,
                apiPolicyVersionSnapshotService,
                templateVersionRepository,
                templateAdGroupAuthorizationCache,
                apiPolicyImpactPreviewService,
                apiPolicyViewMapper,
                access
        );
    }

    @Transactional(readOnly = true)
    public ApiPolicyView getPolicy(UUID templateId, ManagementSessionClaims session) {
        access.requireApiAdmin(templateId, session);
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(templateId)
                .orElseThrow(ApiManagementNotFoundException::new);
        return apiPolicyViewMapper.toPolicyView(policy);
    }

    @Transactional(readOnly = true)
    public ContractResultView getCallerContract(
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

    @Transactional(readOnly = true)
    public ApiRoutesSummaryView getRoutesSummary(
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

    @Transactional
    public ApiPolicyView upsertPolicy(UUID templateId, UpsertApiPolicyRequest request, ManagementSessionClaims session) {
        return policies.upsertPolicy(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveAdGroupsDomain(UUID templateId, SaveAdGroupsRequest request, ManagementSessionClaims session) {
        return policies.saveAdGroupsDomain(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveOutputDomain(UUID templateId, SaveOutputPolicyRequest request, ManagementSessionClaims session) {
        return policies.saveOutputDomain(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveBatchLimitsDomain(
            UUID templateId,
            SaveBatchLimitsRequest request,
            ManagementSessionClaims session
    ) {
        return policies.saveBatchLimitsDomain(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveEncryptionDomain(
            UUID templateId,
            SaveEncryptionPolicyRequest request,
            ManagementSessionClaims session
    ) {
        return policies.saveEncryptionDomain(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveInvocationRetentionDomain(
            UUID templateId,
            SaveInvocationRetentionRequest request,
            ManagementSessionClaims session
    ) {
        return policies.saveInvocationRetentionDomain(templateId, request, session);
    }

    @Transactional
    public ApiPolicyView saveDefaultRouteDomain(
            UUID templateId,
            SaveDefaultRouteRequest request,
            ManagementSessionClaims session
    ) {
        return policies.saveDefaultRouteDomain(templateId, request, session);
    }

    @Transactional(readOnly = true)
    public List<ApiCredentialSummaryView> listCredentials(UUID templateId, ManagementSessionClaims session) {
        return credentials.listCredentials(templateId, session);
    }

    @Transactional
    public ApiCredentialCreatedView createCredential(UUID templateId, ManagementSessionClaims session) {
        return credentials.createCredential(templateId, session);
    }

    @Transactional
    public RotateCredentialResponse rotateCredential(
            UUID templateId,
            UUID credentialId,
            ManagementSessionClaims session
    ) {
        return credentials.rotateCredential(templateId, credentialId, session);
    }

    @Transactional
    public ApiCredentialSummaryView revokeCredential(UUID templateId, UUID credentialId, ManagementSessionClaims session) {
        return credentials.revokeCredential(templateId, credentialId, session);
    }
}
