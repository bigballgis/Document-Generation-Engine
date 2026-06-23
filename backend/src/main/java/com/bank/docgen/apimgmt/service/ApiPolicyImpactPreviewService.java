package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiPolicyImpactPreviewService {

    private final TemplateService templateService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final GroupAccessService groupAccessService;
    private final ObjectMapper objectMapper;

    public ApiPolicyImpactPreviewService(
            TemplateService templateService,
            ApiPolicyRepository apiPolicyRepository,
            TemplateVersionRepository templateVersionRepository,
            GroupAccessService groupAccessService,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.groupAccessService = groupAccessService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ApiPolicyImpactPreviewView preview(
            UUID templateId,
            UpsertApiPolicyRequest request,
            ManagementSessionClaims session
    ) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);

        String allowedJson = writeJson(request.allowedAdGroups());
        String outputFormatsJson = writeJson(request.outputFormats());
        String outputModesJson = writeJson(request.outputModes());

        int currentPolicyVersion = existing.map(ApiPolicyEntity::getPolicyVersion).orElse(0);
        int nextPolicyVersion = currentPolicyVersion + 1;
        List<String> changedAreas = existing
                .map(policy -> ApiPolicyChangeAreaResolver.detectChangedAreas(
                        policy,
                        request,
                        allowedJson,
                        outputFormatsJson,
                        outputModesJson
                ))
                .orElseGet(ApiPolicyChangeAreaResolver::initialChangedAreas);

        List<String> callableReleaseVersions = resolveCallableReleaseVersions(template);
        String candidateDefaultRoute = request.defaultRouteReleaseVersion();
        boolean hasCandidateDefaultRoute = candidateDefaultRoute != null && !candidateDefaultRoute.isBlank();
        boolean blocking = hasCandidateDefaultRoute && !callableReleaseVersions.contains(candidateDefaultRoute);
        boolean defaultRouteImpacted = changedAreas.contains("DEFAULT_ROUTE_TARGET");
        List<String> warnings = buildWarnings(blocking, defaultRouteImpacted);

        return new ApiPolicyImpactPreviewView(
                changedAreas,
                blocking,
                warnings,
                defaultRouteImpacted,
                currentPolicyVersion,
                nextPolicyVersion,
                summaryMessageKey(blocking, warnings.isEmpty())
        );
    }

    private List<String> resolveCallableReleaseVersions(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            return List.of();
        }
        String releaseVersion = template.getReleaseVersion();
        if (releaseVersion == null || releaseVersion.isBlank()) {
            return List.of();
        }
        return templateVersionRepository.findByTemplateIdAndReleaseVersion(template.getId(), releaseVersion)
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED)
                .map(version -> List.of(releaseVersion))
                .orElseGet(List::of);
    }

    private List<String> buildWarnings(boolean blocking, boolean defaultRouteImpacted) {
        List<String> warnings = new ArrayList<>();
        if (defaultRouteImpacted) {
            warnings.add("api.apimgmt.policyImpact.defaultRouteChanged");
        }
        if (blocking) {
            warnings.add("api.apimgmt.policyImpact.defaultRouteNotCallable");
        }
        return warnings;
    }

    private String summaryMessageKey(boolean blocking, boolean warningFree) {
        if (blocking) {
            return "api.apimgmt.policyImpact.blocking";
        }
        if (warningFree) {
            return "api.apimgmt.policyImpact.safe";
        }
        return "api.apimgmt.policyImpact.warning";
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
