package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.apimgmt.api.SaveDefaultRouteRequest;
import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
        requireApiAdmin(session);
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
        String currentDefaultRoute = existing.map(ApiPolicyEntity::getDefaultRouteReleaseVersion).orElse(null);
        boolean hasCandidateDefaultRoute = candidateDefaultRoute != null && !candidateDefaultRoute.isBlank();
        boolean blocking = hasCandidateDefaultRoute && !callableReleaseVersions.contains(candidateDefaultRoute);
        boolean defaultRouteImpacted = changedAreas.contains("DEFAULT_ROUTE_TARGET");
        List<String> warnings = buildWarnings(blocking, defaultRouteImpacted, existing, request);
        return new ApiPolicyImpactPreviewView(
                changedAreas,
                blocking,
                warnings,
                defaultRouteImpacted,
                currentPolicyVersion,
                nextPolicyVersion,
                summaryMessageKey(blocking, warnings.isEmpty()),
                null,
                defaultRouteImpacted ? "api.apimgmt.policyImpact.idempotencyDefaultRouteGuard" : null,
                currentDefaultRoute,
                candidateDefaultRoute
        );
    }

    @Transactional(readOnly = true)
    public ApiPolicyImpactPreviewView previewDefaultRoute(
            UUID templateId,
            SaveDefaultRouteRequest request,
            ManagementSessionClaims session
    ) {
        requireApiAdmin(session);
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);
        int currentPolicyVersion = existing.map(ApiPolicyEntity::getPolicyVersion).orElse(0);
        String candidateDefaultRoute = request.defaultRouteReleaseVersion();
        String currentDefaultRoute = existing.map(ApiPolicyEntity::getDefaultRouteReleaseVersion).orElse(null);
        boolean defaultRouteImpacted = !Objects.equals(currentDefaultRoute, candidateDefaultRoute);
        List<String> callableReleaseVersions = resolveCallableReleaseVersions(template);
        boolean blocking = candidateDefaultRoute != null
                && !candidateDefaultRoute.isBlank()
                && !callableReleaseVersions.contains(candidateDefaultRoute);
        List<String> warnings = buildWarnings(blocking, defaultRouteImpacted, existing, null);

        return new ApiPolicyImpactPreviewView(
                List.of("DEFAULT_ROUTE_TARGET"),
                blocking,
                warnings,
                defaultRouteImpacted,
                currentPolicyVersion,
                currentPolicyVersion + 1,
                summaryMessageKey(blocking, warnings.isEmpty()),
                null,
                defaultRouteImpacted ? "api.apimgmt.policyImpact.idempotencyDefaultRouteGuard" : null,
                currentDefaultRoute,
                candidateDefaultRoute
        );
    }

    private void requireApiAdmin(ManagementSessionClaims session) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
    }

    private List<String> resolveCallableReleaseVersions(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            return List.of();
        }
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(template.getId()).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED)
                .map(TemplateVersionEntity::getReleaseVersion)
                .filter(releaseVersion -> releaseVersion != null && !releaseVersion.isBlank())
                .toList();
    }

    /**
     * FOS-W11-1: warn when allowances narrow (formats/modes removed, batch lowered, AD groups removed).
     */
    private List<String> buildWarnings(
            boolean blocking,
            boolean defaultRouteImpacted,
            Optional<ApiPolicyEntity> existing,
            UpsertApiPolicyRequest request
    ) {
        List<String> warnings = new ArrayList<>();
        if (defaultRouteImpacted) {
            warnings.add("api.apimgmt.policyImpact.defaultRouteChanged");
        }
        if (blocking) {
            warnings.add("api.apimgmt.policyImpact.defaultRouteNotCallable");
        }
        if (existing.isPresent() && request != null) {
            ApiPolicyEntity policy = existing.get();
            if (isStrictSubset(readStringList(policy.getOutputFormatsJson()), request.outputFormats())) {
                warnings.add("api.apimgmt.policyImpact.outputFormatsNarrowed");
            }
            if (isStrictSubset(readStringList(policy.getOutputModesJson()), request.outputModes())) {
                warnings.add("api.apimgmt.policyImpact.outputModesNarrowed");
            }
            if (isStrictSubset(readStringList(policy.getAllowedAdGroupsJson()), request.allowedAdGroups())) {
                warnings.add("api.apimgmt.policyImpact.adGroupsNarrowed");
            }
            int currentBatch = Math.max(policy.getMaxBatchSize(), policy.getBatchSyncMaxItems());
            if (request.maxBatchSize() < currentBatch) {
                warnings.add("api.apimgmt.policyImpact.batchLimitLowered");
            }
        }
        return warnings;
    }

    private static boolean isStrictSubset(List<String> current, List<String> candidate) {
        Set<String> currentSet = toUpperSet(current);
        Set<String> candidateSet = toUpperSet(candidate);
        return !candidateSet.containsAll(currentSet) && !currentSet.equals(candidateSet);
    }

    private static Set<String> toUpperSet(List<String> values) {
        Set<String> out = new HashSet<>();
        if (values == null) {
            return out;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                out.add(value.trim().toUpperCase(Locale.ROOT));
            }
        }
        return out;
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            return List.of();
        }
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
