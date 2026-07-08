package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.AuditPagedResult;
import com.bank.docgen.audit.api.LifecycleAuditExportResult;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.api.ManagementAuditEventView;
import com.bank.docgen.audit.api.ManagementAuditExportEventView;
import com.bank.docgen.audit.api.ManagementAuditExportResult;
import com.bank.docgen.audit.api.ManagementAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.api.LifecycleAuditEventView;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TemplateService.TemplateDisplayInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    public static final String EXPORT_FORMAT = "management-audit-export-v1-json";
    public static final String LIFECYCLE_EXPORT_FORMAT = "lifecycle-audit-export-v1-json";

    private final ManagementAuditEventRepository managementAuditEventRepository;
    private final RuntimeGenerationAuditEventRepository runtimeGenerationAuditEventRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final TemplateService templateService;
    private final ManagementUserDisplayService managementUserDisplayService;
    private final GroupAccessService groupAccessService;
    private final AuditMaskingService auditMaskingService;
    private final ObjectMapper objectMapper;

    public AuditQueryService(
            ManagementAuditEventRepository managementAuditEventRepository,
            RuntimeGenerationAuditEventRepository runtimeGenerationAuditEventRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            TemplateService templateService,
            ManagementUserDisplayService managementUserDisplayService,
            GroupAccessService groupAccessService,
            AuditMaskingService auditMaskingService,
            ObjectMapper objectMapper
    ) {
        this.managementAuditEventRepository = managementAuditEventRepository;
        this.runtimeGenerationAuditEventRepository = runtimeGenerationAuditEventRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.templateService = templateService;
        this.managementUserDisplayService = managementUserDisplayService;
        this.groupAccessService = groupAccessService;
        this.auditMaskingService = auditMaskingService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ManagementAuditQueryResult queryManagementEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope,
            String requestId,
            Integer page,
            Integer size
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = resolveGroupFilter(session, actorRole, templateId, groupScope);
        String normalizedRequestId = normalizeRequestId(requestId);
        int safePage = AuditPagedResult.normalizePage(page);
        int safeSize = AuditPagedResult.normalizeSize(size);
        List<ManagementAuditEventView> events;
        long totalElements;
        int totalPages;
        if (normalizedRequestId != null) {
            AuditSearchPage<RuntimeGenerationAuditEventEntity> searchPage =
                    runtimeGenerationAuditEventRepository.searchPaged(
                            templateId,
                            eventType,
                            credentialId,
                            eventAtFrom,
                            eventAtTo,
                            groupFilter,
                            normalizedRequestId,
                            safePage,
                            safeSize
                    );
            events = toManagementViewsFromRuntime(searchPage.content());
            totalElements = searchPage.totalElements();
            totalPages = searchPage.totalPages();
        } else {
            AuditSearchPage<ManagementAuditEventEntity> searchPage = managementAuditEventRepository.searchPaged(
                    templateId,
                    eventType,
                    credentialId,
                    eventAtFrom,
                    eventAtTo,
                    groupFilter,
                    safePage,
                    safeSize
            );
            events = toManagementViews(searchPage.content());
            totalElements = searchPage.totalElements();
            totalPages = searchPage.totalPages();
        }
        return new ManagementAuditQueryResult(
                events,
                safePage,
                safeSize,
                totalElements,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public ManagementAuditExportResult exportManagementEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope,
            String requestId
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = resolveGroupFilter(session, actorRole, templateId, groupScope);
        String normalizedRequestId = normalizeRequestId(requestId);
        List<ManagementAuditExportEventView> events;
        if (normalizedRequestId != null) {
            events = runtimeGenerationAuditEventRepository.search(
                    templateId,
                    eventType,
                    credentialId,
                    eventAtFrom,
                    eventAtTo,
                    groupFilter,
                    normalizedRequestId
            ).stream().map(this::toRuntimeExportView).toList();
        } else {
            events = managementAuditEventRepository.search(
                    templateId,
                    eventType,
                    credentialId,
                    eventAtFrom,
                    eventAtTo,
                    groupFilter
            ).stream().map(this::toExportView).toList();
        }
        return new ManagementAuditExportResult(EXPORT_FORMAT, events);
    }

    @Transactional(readOnly = true)
    public LifecycleAuditQueryResult queryLifecycleEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope,
            String requestId,
            Integer page,
            Integer size
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        if (!groupAccessService.canReadAudit(session)) {
            throw new AuditAccessDeniedException();
        }
        validateActorRole(session, actorRole);

        int safePage = AuditPagedResult.normalizePage(page);
        int safeSize = AuditPagedResult.normalizeSize(size);
        if (normalizeRequestId(requestId) != null) {
            return new LifecycleAuditQueryResult(List.of(), safePage, safeSize, 0, 0);
        }
        UUID scopedTemplateId = resolveLifecycleTemplateId(session, actorRole, templateId, groupScope);
        AuditSearchPage<TemplateLifecycleRecordEntity> searchPage = lifecycleRecordRepository.searchPaged(
                scopedTemplateId,
                eventType,
                eventAtFrom,
                eventAtTo,
                safePage,
                safeSize
        );
        List<LifecycleAuditEventView> events = toLifecycleViews(searchPage.content());
        return new LifecycleAuditQueryResult(
                events,
                safePage,
                safeSize,
                searchPage.totalElements(),
                searchPage.totalPages()
        );
    }

    private UUID resolveLifecycleTemplateId(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String groupScope
    ) {
        if (actorRole == AuditReadActorRole.GROUP_ADMIN) {
            resolveGroupFilter(session, actorRole, templateId, groupScope);
            TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
            return template.getId();
        }
        if (templateId != null) {
            TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
            return template.getId();
        }
        return null;
    }

    private List<LifecycleAuditEventView> queryAllLifecycleEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope
    ) {
        UUID scopedTemplateId = resolveLifecycleTemplateId(session, actorRole, templateId, groupScope);
        List<TemplateLifecycleRecordEntity> records = scopedTemplateId != null
                ? lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(scopedTemplateId)
                : lifecycleRecordRepository.findAllByOrderByCreatedAtDesc();
        List<TemplateLifecycleRecordEntity> filtered = records.stream()
                .filter(record -> eventType == null || record.getAction().name().equals(eventType))
                .filter(record -> eventAtFrom == null || !record.getCreatedAt().isBefore(eventAtFrom))
                .filter(record -> eventAtTo == null || !record.getCreatedAt().isAfter(eventAtTo))
                .toList();
        return toLifecycleViews(filtered);
    }

    private List<ManagementAuditEventView> toManagementViews(List<ManagementAuditEventEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = entities.stream()
                .map(ManagementAuditEventEntity::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        return entities.stream()
                .map(entity -> toManagementView(entity, templateDisplayInfo.get(entity.getTemplateId())))
                .toList();
    }

    private List<LifecycleAuditEventView> toLifecycleViews(List<TemplateLifecycleRecordEntity> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = records.stream()
                .map(TemplateLifecycleRecordEntity::getTemplateId)
                .collect(Collectors.toSet());
        Set<String> actorUsernames = records.stream()
                .map(TemplateLifecycleRecordEntity::getActorUsername)
                .filter(username -> username != null && !username.isBlank())
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        Map<String, String> actorDisplayNames = managementUserDisplayService.lookupDisplayNames(actorUsernames);
        return records.stream()
                .map(record -> toLifecycleView(
                        record.getTemplateId(),
                        record,
                        templateDisplayInfo.get(record.getTemplateId()),
                        resolveActorDisplayName(record.getActorUsername(), actorDisplayNames)
                ))
                .toList();
    }

    private static String resolveActorDisplayName(String username, Map<String, String> actorDisplayNames) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return actorDisplayNames.get(username);
    }

    @Transactional(readOnly = true)
    public LifecycleAuditExportResult exportLifecycleEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope,
            String requestId
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        if (!groupAccessService.canReadAudit(session)) {
            throw new AuditAccessDeniedException();
        }
        validateActorRole(session, actorRole);
        if (normalizeRequestId(requestId) != null) {
            return new LifecycleAuditExportResult(LIFECYCLE_EXPORT_FORMAT, List.of());
        }
        List<LifecycleAuditEventView> events = queryAllLifecycleEvents(
                session,
                actorRole,
                templateId,
                eventType,
                eventAtFrom,
                eventAtTo,
                groupScope
        );
        return new LifecycleAuditExportResult(LIFECYCLE_EXPORT_FORMAT, events);
    }

    private String resolveGroupFilter(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String groupScope
    ) {
        if (!groupAccessService.canReadAudit(session)) {
            throw new AuditAccessDeniedException();
        }
        validateActorRole(session, actorRole);
        return switch (actorRole) {
            case AUDIT_ADMIN, GLOBAL_ADMIN -> null;
            case GROUP_ADMIN -> resolveGroupAdminScope(session, templateId, groupScope);
        };
    }

    private void validateActorRole(ManagementSessionClaims session, AuditReadActorRole actorRole) {
        switch (actorRole) {
            case AUDIT_ADMIN -> {
                if (!session.roles().contains("AUDIT_ADMIN") && !session.roles().contains("GLOBAL_ADMIN")) {
                    throw new AuditAccessDeniedException();
                }
            }
            case GLOBAL_ADMIN -> {
                if (!session.roles().contains("GLOBAL_ADMIN")) {
                    throw new AuditAccessDeniedException();
                }
            }
            case GROUP_ADMIN -> {
                if (!session.roles().contains("GROUP_ADMIN") && !session.roles().contains("GLOBAL_ADMIN")) {
                    throw new AuditAccessDeniedException();
                }
            }
            default -> throw new AuditAccessDeniedException();
        }
    }

    private String resolveGroupAdminScope(
            ManagementSessionClaims session,
            UUID templateId,
            String groupScope
    ) {
        if (templateId == null || groupScope == null || groupScope.isBlank()) {
            throw new AuditValidationException(
                    ApiErrorCodes.AUDIT_SCOPE_REQUIRED,
                    "api.error.audit.scopeRequired"
            );
        }
        if (!session.authorizedGroupCodes().contains(groupScope) && !session.roles().contains("GLOBAL_ADMIN")) {
            throw new AuditAccessDeniedException();
        }
        TemplateEntity template;
        try {
            template = templateService.requireReadableTemplate(templateId, session);
        } catch (TemplateNotFoundException ex) {
            throw ex;
        }
        if (!template.getGroupCode().equals(groupScope)) {
            throw new AuditAccessDeniedException();
        }
        return groupScope;
    }

    private void validateTimeWindow(Instant eventAtFrom, Instant eventAtTo) {
        if (eventAtFrom != null && eventAtTo != null && eventAtFrom.isAfter(eventAtTo)) {
            throw new AuditValidationException(
                    ApiErrorCodes.INVALID_TIME_WINDOW,
                    "api.error.audit.invalidTimeWindow"
            );
        }
    }

    private ManagementAuditEventView toManagementView(
            ManagementAuditEventEntity entity,
            TemplateDisplayInfo templateDisplayInfo
    ) {
        return new ManagementAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                entity.getPreviousPolicyVersion(),
                entity.getPolicyVersion(),
                readStringList(entity.getChangedAreasJson()),
                entity.isRollback(),
                entity.getRollbackSourcePolicyVersion(),
                entity.getActorSummary(),
                entity.getCredentialFingerprint(),
                entity.getStatusSummary(),
                readStringList(entity.getWarningCodesJson()),
                null
        );
    }

    private List<ManagementAuditEventView> toManagementViewsFromRuntime(
            List<RuntimeGenerationAuditEventEntity> entities
    ) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<UUID> templateIds = entities.stream()
                .map(RuntimeGenerationAuditEventEntity::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, TemplateDisplayInfo> templateDisplayInfo = templateService.lookupDisplayInfoByIds(templateIds);
        return entities.stream()
                .map(entity -> toManagementViewFromRuntime(
                        entity,
                        templateDisplayInfo.get(entity.getTemplateId())
                ))
                .toList();
    }

    private ManagementAuditEventView toManagementViewFromRuntime(
            RuntimeGenerationAuditEventEntity entity,
            TemplateDisplayInfo templateDisplayInfo
    ) {
        String statusSummary = entity.getResultSummary();
        if (statusSummary == null || statusSummary.isBlank()) {
            statusSummary = entity.getOutcome();
        }
        return new ManagementAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                null,
                null,
                List.of(),
                false,
                null,
                entity.getAccessAccount(),
                entity.getCredentialFingerprint(),
                statusSummary,
                List.of(),
                entity.getRequestId()
        );
    }

    private ManagementAuditExportEventView toRuntimeExportView(RuntimeGenerationAuditEventEntity entity) {
        String statusSummary = entity.getResultSummary();
        if (statusSummary == null || statusSummary.isBlank()) {
            statusSummary = entity.getOutcome();
        }
        return new ManagementAuditExportEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                null,
                null,
                List.of(),
                false,
                null,
                auditMaskingService.maskActorSummary(entity.getAccessAccount()),
                auditMaskingService.maskCredentialFingerprint(entity.getCredentialFingerprint()),
                statusSummary,
                List.of()
        );
    }

    private static String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
    }

    private ManagementAuditExportEventView toExportView(ManagementAuditEventEntity entity) {
        return new ManagementAuditExportEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                entity.getPreviousPolicyVersion(),
                entity.getPolicyVersion(),
                readStringList(entity.getChangedAreasJson()),
                entity.isRollback(),
                entity.getRollbackSourcePolicyVersion(),
                auditMaskingService.maskActorSummary(entity.getActorSummary()),
                auditMaskingService.maskCredentialFingerprint(entity.getCredentialFingerprint()),
                entity.getStatusSummary(),
                readStringList(entity.getWarningCodesJson())
        );
    }

    private LifecycleAuditEventView toLifecycleView(
            UUID templateId,
            TemplateLifecycleRecordEntity record,
            TemplateDisplayInfo templateDisplayInfo,
            String actorDisplayName
    ) {
        return new LifecycleAuditEventView(
                record.getCreatedAt(),
                record.getAction().name(),
                templateId.toString(),
                templateDisplayInfo == null ? null : templateDisplayInfo.name(),
                templateDisplayInfo == null ? null : templateDisplayInfo.externalId(),
                record.getAction().name(),
                record.getFromStatus() == null ? null : record.getFromStatus().name(),
                record.getToStatus() == null ? null : record.getToStatus().name(),
                record.getActorUsername(),
                actorDisplayName,
                record.getCommentSummary(),
                List.of()
        );
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
