package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiAccessAlertView;
import com.bank.docgen.apimgmt.api.ApiAccessReadinessSummaryView;
import com.bank.docgen.apimgmt.domain.ApiAccessAlertSeverity;
import com.bank.docgen.apimgmt.domain.ApiAccessAlertType;
import com.bank.docgen.apimgmt.domain.ApiCredentialLifecycleSupport;
import com.bank.docgen.apimgmt.persistence.ApiCredentialEntity;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiAccessAlertQueryService {

    private static final String MISSING_AD_GROUP_MESSAGE_KEY = "apiPolicy.home.alerts.missingAdGroup";
    private static final String EXPIRING_CREDENTIAL_MESSAGE_KEY = "apiPolicy.home.alerts.expiringCredential";
    private static final String NO_CREDENTIALS_MESSAGE_KEY = "apiPolicy.home.alerts.noCredentials";

    private final GroupAccessService groupAccessService;
    private final TemplateRepository templateRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final ApiCredentialRepository apiCredentialRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiAccessAlertQueryService(
            GroupAccessService groupAccessService,
            TemplateRepository templateRepository,
            ApiPolicyRepository apiPolicyRepository,
            ApiCredentialRepository apiCredentialRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.groupAccessService = groupAccessService;
        this.templateRepository = templateRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.apiCredentialRepository = apiCredentialRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ApiAccessAlertView> listAlerts(ManagementSessionClaims session) {
        requireApiAdmin(session);
        return buildAlerts(loadScopedPublishedAndPending(session));
    }

    @Transactional(readOnly = true)
    public ApiAccessReadinessSummaryView readinessSummary(ManagementSessionClaims session) {
        requireApiAdmin(session);
        ScopedTemplates scoped = loadScopedPublishedAndPending(session);
        List<ApiAccessAlertView> alerts = buildAlerts(scoped);
        Set<UUID> attentionTemplateIds = new HashSet<>();
        Set<UUID> pendingIds = scoped.pendingRelease().stream()
                .map(TemplateEntity::getId)
                .collect(Collectors.toSet());
        Set<UUID> pendingNeedingSetupIds = new HashSet<>();
        for (ApiAccessAlertView alert : alerts) {
            attentionTemplateIds.add(alert.templateId());
            if (alert.alertType() == ApiAccessAlertType.MISSING_AD_GROUP
                    && pendingIds.contains(alert.templateId())) {
                pendingNeedingSetupIds.add(alert.templateId());
            }
        }
        return new ApiAccessReadinessSummaryView(
                scoped.published().size(),
                attentionTemplateIds.size(),
                pendingNeedingSetupIds.size()
        );
    }

    private List<ApiAccessAlertView> buildAlerts(ScopedTemplates scoped) {
        List<TemplateEntity> candidates = scoped.all();
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<UUID> templateIds = scoped.allTemplateIds();
        Map<UUID, ApiPolicyEntity> policiesByTemplateId = loadPolicies(templateIds);
        Map<UUID, List<ApiCredentialEntity>> credentialsByTemplateId = apiCredentialRepository
                .findByTemplateIdIn(templateIds).stream()
                .collect(Collectors.groupingBy(ApiCredentialEntity::getTemplateId));

        Instant now = clock.instant();
        List<ApiAccessAlertView> alerts = new ArrayList<>();
        for (TemplateEntity template : candidates) {
            UUID templateId = template.getId();
            ApiPolicyEntity policy = policiesByTemplateId.get(templateId);
            List<ApiCredentialEntity> credentials = credentialsByTemplateId.getOrDefault(templateId, List.of());
            boolean published = template.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED;

            // AOD-C6: MISSING_AD_GROUP covers PUBLISHED ∪ PENDING_RELEASE
            if (policy != null && readStringList(policy.getAllowedAdGroupsJson()).isEmpty()) {
                alerts.add(new ApiAccessAlertView(
                        ApiAccessAlertType.MISSING_AD_GROUP,
                        ApiAccessAlertSeverity.WARNING,
                        templateId,
                        template.getExternalId(),
                        template.getName(),
                        template.getGroupCode(),
                        MISSING_AD_GROUP_MESSAGE_KEY,
                        hubDeepLinkPath(templateId, "AD_GROUP_AUTHORIZATION"),
                        null,
                        null
                ));
            }

            // AOD-C6: credential alerts remain PUBLISHED-only
            if (!published) {
                continue;
            }

            List<ApiCredentialEntity> activeCredentials = credentials.stream()
                    .filter(credential -> ApiCredentialLifecycleSupport.isActiveCredential(credential, now))
                    .toList();
            if (activeCredentials.isEmpty()) {
                alerts.add(new ApiAccessAlertView(
                        ApiAccessAlertType.NO_CREDENTIALS,
                        ApiAccessAlertSeverity.WARNING,
                        templateId,
                        template.getExternalId(),
                        template.getName(),
                        template.getGroupCode(),
                        NO_CREDENTIALS_MESSAGE_KEY,
                        hubDeepLinkPath(templateId, null),
                        null,
                        null
                ));
            }

            credentials.stream()
                    .filter(credential -> ApiCredentialLifecycleSupport.isExpiringCredential(credential, now))
                    .forEach(credential -> alerts.add(new ApiAccessAlertView(
                            ApiAccessAlertType.EXPIRING_CREDENTIAL,
                            ApiAccessAlertSeverity.WARNING,
                            templateId,
                            template.getExternalId(),
                            template.getName(),
                            template.getGroupCode(),
                            EXPIRING_CREDENTIAL_MESSAGE_KEY,
                            hubDeepLinkPath(templateId, null),
                            credential.getExternalId(),
                            ApiCredentialLifecycleSupport.resolveExpiresAt(credential)
                    )));
        }

        alerts.sort(Comparator
                .comparing(ApiAccessAlertView::severity)
                .thenComparing(ApiAccessAlertView::templateName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(alert -> alert.alertType().name()));
        return List.copyOf(alerts);
    }

    private Map<UUID, ApiPolicyEntity> loadPolicies(List<UUID> templateIds) {
        if (templateIds.isEmpty()) {
            return Map.of();
        }
        return apiPolicyRepository.findByTemplateIdIn(templateIds).stream()
                .collect(Collectors.toMap(ApiPolicyEntity::getTemplateId, Function.identity()));
    }

    private ScopedTemplates loadScopedPublishedAndPending(ManagementSessionClaims session) {
        List<String> groupCodes = groupAccessService.accessibleGroupCodes(session);
        if (groupCodes.contains("*")) {
            return new ScopedTemplates(
                    templateRepository.findByDeletedAtIsNullAndLifecycleStatusOrderByUpdatedAtDesc(
                            TemplateLifecycleStatus.PUBLISHED
                    ),
                    templateRepository.findByDeletedAtIsNullAndLifecycleStatusOrderByUpdatedAtDesc(
                            TemplateLifecycleStatus.PENDING_RELEASE
                    )
            );
        }
        if (groupCodes.isEmpty()) {
            return ScopedTemplates.empty();
        }
        return new ScopedTemplates(
                templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                        groupCodes,
                        TemplateLifecycleStatus.PUBLISHED
                ),
                templateRepository.findByDeletedAtIsNullAndGroupCodeInAndLifecycleStatusOrderByUpdatedAtDesc(
                        groupCodes,
                        TemplateLifecycleStatus.PENDING_RELEASE
                )
        );
    }

    private void requireApiAdmin(ManagementSessionClaims session) {
        if (!groupAccessService.canManageApiPolicy(session)) {
            throw new ApiManagementAccessDeniedException();
        }
    }

    private static String hubDeepLinkPath(UUID templateId, String domain) {
        String base = "/templates/" + templateId + "?tab=apiAccess";
        if (domain == null || domain.isBlank()) {
            return base;
        }
        return base + "#domain=" + domain;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return values == null ? List.of() : values;
        } catch (JsonProcessingException ex) {
            // Alert aggregation is best-effort: malformed policy JSON must not 500 the home surface.
            return List.of();
        }
    }

    private record ScopedTemplates(
            List<TemplateEntity> published,
            List<TemplateEntity> pendingRelease
    ) {
        static ScopedTemplates empty() {
            return new ScopedTemplates(List.of(), List.of());
        }

        List<TemplateEntity> all() {
            if (published.isEmpty()) {
                return List.copyOf(pendingRelease);
            }
            if (pendingRelease.isEmpty()) {
                return List.copyOf(published);
            }
            List<TemplateEntity> combined = new ArrayList<>(published.size() + pendingRelease.size());
            combined.addAll(published);
            combined.addAll(pendingRelease);
            return List.copyOf(combined);
        }

        List<UUID> allTemplateIds() {
            return all().stream().map(TemplateEntity::getId).toList();
        }
    }
}
