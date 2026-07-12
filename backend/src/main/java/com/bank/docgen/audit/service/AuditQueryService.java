package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.GenerationAuditEventView;
import com.bank.docgen.audit.api.GenerationAuditQueryResult;
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
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    private final GroupAccessService groupAccessService;
    private final AuditQueryAccessSupport accessSupport;
    private final AuditEventViewMapper viewMapper;

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
        this.groupAccessService = groupAccessService;
        this.accessSupport = new AuditQueryAccessSupport(groupAccessService, templateService);
        this.viewMapper = new AuditEventViewMapper(
                templateService,
                managementUserDisplayService,
                auditMaskingService,
                objectMapper
        );
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
        accessSupport.validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = accessSupport.resolveGroupFilter(session, actorRole, templateId, groupScope);
        String normalizedRequestId = AuditQueryAccessSupport.normalizeRequestId(requestId);
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
            events = viewMapper.toManagementViewsFromRuntime(searchPage.content());
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
            events = viewMapper.toManagementViews(searchPage.content());
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
    public GenerationAuditQueryResult queryGenerationEventsByExternalId(
            ManagementSessionClaims session,
            String templateExternalId,
            Integer page,
            Integer size
    ) {
        if (!groupAccessService.canReadAudit(session)) {
            throw new AuditAccessDeniedException();
        }
        TemplateEntity template = templateService.requireTemplateByExternalId(templateExternalId.trim());
        templateService.requireReadableTemplate(template.getId(), session);
        int safePage = AuditPagedResult.normalizePage(page);
        int safeSize = AuditPagedResult.normalizeSize(size);
        AuditSearchPage<RuntimeGenerationAuditEventEntity> searchPage =
                runtimeGenerationAuditEventRepository.searchPaged(
                        template.getId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        safePage,
                        safeSize
                );
        List<GenerationAuditEventView> content = searchPage.content().stream()
                .map(entity -> viewMapper.toGenerationAuditEventView(entity, template.getExternalId()))
                .toList();
        return new GenerationAuditQueryResult(
                content,
                safePage,
                safeSize,
                searchPage.totalElements(),
                searchPage.totalPages()
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
        accessSupport.validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = accessSupport.resolveGroupFilter(session, actorRole, templateId, groupScope);
        String normalizedRequestId = AuditQueryAccessSupport.normalizeRequestId(requestId);
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
            ).stream().map(viewMapper::toRuntimeExportView).toList();
        } else {
            events = managementAuditEventRepository.search(
                    templateId,
                    eventType,
                    credentialId,
                    eventAtFrom,
                    eventAtTo,
                    groupFilter
            ).stream().map(viewMapper::toExportView).toList();
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
        accessSupport.validateTimeWindow(eventAtFrom, eventAtTo);
        accessSupport.requireCanReadAudit(session);
        accessSupport.validateActorRole(session, actorRole);

        int safePage = AuditPagedResult.normalizePage(page);
        int safeSize = AuditPagedResult.normalizeSize(size);
        if (AuditQueryAccessSupport.normalizeRequestId(requestId) != null) {
            return new LifecycleAuditQueryResult(List.of(), safePage, safeSize, 0, 0);
        }
        UUID scopedTemplateId = accessSupport.resolveLifecycleTemplateId(session, actorRole, templateId, groupScope);
        AuditSearchPage<TemplateLifecycleRecordEntity> searchPage = lifecycleRecordRepository.searchPaged(
                scopedTemplateId,
                eventType,
                eventAtFrom,
                eventAtTo,
                safePage,
                safeSize
        );
        List<LifecycleAuditEventView> events = viewMapper.toLifecycleViews(searchPage.content());
        return new LifecycleAuditQueryResult(
                events,
                safePage,
                safeSize,
                searchPage.totalElements(),
                searchPage.totalPages()
        );
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
        accessSupport.validateTimeWindow(eventAtFrom, eventAtTo);
        accessSupport.requireCanReadAudit(session);
        accessSupport.validateActorRole(session, actorRole);
        if (AuditQueryAccessSupport.normalizeRequestId(requestId) != null) {
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

    private List<LifecycleAuditEventView> queryAllLifecycleEvents(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupScope
    ) {
        UUID scopedTemplateId = accessSupport.resolveLifecycleTemplateId(session, actorRole, templateId, groupScope);
        List<TemplateLifecycleRecordEntity> records = scopedTemplateId != null
                ? lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(scopedTemplateId)
                : lifecycleRecordRepository.findAllByOrderByCreatedAtDesc();
        List<TemplateLifecycleRecordEntity> filtered = records.stream()
                .filter(record -> eventType == null || record.getAction().name().equals(eventType))
                .filter(record -> eventAtFrom == null || !record.getCreatedAt().isBefore(eventAtFrom))
                .filter(record -> eventAtTo == null || !record.getCreatedAt().isAfter(eventAtTo))
                .toList();
        return viewMapper.toLifecycleViews(filtered);
    }
}
