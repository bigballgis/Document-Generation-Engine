package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.AuditPagedResult;
import com.bank.docgen.audit.api.LifecycleAuditEventView;
import com.bank.docgen.audit.api.LifecycleAuditExportResult;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Package-private lifecycle audit query / export bodies.
 */
final class AuditQueryLifecycleSupport {

    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final AuditQueryAccessSupport accessSupport;
    private final AuditEventViewMapper viewMapper;

    AuditQueryLifecycleSupport(
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            AuditQueryAccessSupport accessSupport,
            AuditEventViewMapper viewMapper
    ) {
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.accessSupport = accessSupport;
        this.viewMapper = viewMapper;
    }

    LifecycleAuditQueryResult queryLifecycleEvents(
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

    LifecycleAuditExportResult exportLifecycleEvents(
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
            return new LifecycleAuditExportResult(AuditQueryService.LIFECYCLE_EXPORT_FORMAT, List.of());
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
        return new LifecycleAuditExportResult(AuditQueryService.LIFECYCLE_EXPORT_FORMAT, events);
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
