package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.LifecycleAuditEventView;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.api.ManagementAuditEventView;
import com.bank.docgen.audit.api.ManagementAuditExportEventView;
import com.bank.docgen.audit.api.ManagementAuditExportResult;
import com.bank.docgen.audit.api.ManagementAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    public static final String EXPORT_FORMAT = "management-audit-export-v1-json";

    private final ManagementAuditEventRepository managementAuditEventRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;
    private final TemplateService templateService;
    private final GroupAccessService groupAccessService;
    private final AuditMaskingService auditMaskingService;
    private final ObjectMapper objectMapper;

    public AuditQueryService(
            ManagementAuditEventRepository managementAuditEventRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository,
            TemplateService templateService,
            GroupAccessService groupAccessService,
            AuditMaskingService auditMaskingService,
            ObjectMapper objectMapper
    ) {
        this.managementAuditEventRepository = managementAuditEventRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
        this.templateService = templateService;
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
            String groupScope
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = resolveGroupFilter(session, actorRole, templateId, groupScope);
        List<ManagementAuditEventView> events = managementAuditEventRepository.search(
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupFilter
        ).stream().map(this::toManagementView).toList();
        return new ManagementAuditQueryResult(events);
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
            String groupScope
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        String groupFilter = resolveGroupFilter(session, actorRole, templateId, groupScope);
        List<ManagementAuditExportEventView> events = managementAuditEventRepository.search(
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupFilter
        ).stream().map(this::toExportView).toList();
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
            String groupScope
    ) {
        validateTimeWindow(eventAtFrom, eventAtTo);
        if (templateId == null) {
            throw new AuditValidationException(
                    ApiErrorCodes.AUDIT_SCOPE_REQUIRED,
                    "api.error.audit.scopeRequired"
            );
        }
        resolveGroupFilter(session, actorRole, templateId, groupScope);
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        List<LifecycleAuditEventView> events = lifecycleRecordRepository
                .findByTemplateIdOrderByCreatedAtDesc(template.getId()).stream()
                .filter(record -> eventType == null || record.getAction().name().equals(eventType))
                .filter(record -> eventAtFrom == null || !record.getCreatedAt().isBefore(eventAtFrom))
                .filter(record -> eventAtTo == null || !record.getCreatedAt().isAfter(eventAtTo))
                .map(record -> toLifecycleView(template.getId(), record))
                .toList();
        return new LifecycleAuditQueryResult(events);
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

    private ManagementAuditEventView toManagementView(ManagementAuditEventEntity entity) {
        return new ManagementAuditEventView(
                entity.getEventAt(),
                entity.getEventType(),
                entity.getTemplateId() == null ? null : entity.getTemplateId().toString(),
                entity.getCredentialId() == null ? null : entity.getCredentialId().toString(),
                entity.getPreviousPolicyVersion(),
                entity.getPolicyVersion(),
                readStringList(entity.getChangedAreasJson()),
                entity.isRollback(),
                entity.getRollbackSourcePolicyVersion(),
                entity.getActorSummary(),
                entity.getCredentialFingerprint(),
                entity.getStatusSummary(),
                readStringList(entity.getWarningCodesJson())
        );
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

    private LifecycleAuditEventView toLifecycleView(UUID templateId, TemplateLifecycleRecordEntity record) {
        return new LifecycleAuditEventView(
                record.getCreatedAt(),
                record.getAction().name(),
                templateId.toString(),
                record.getAction().name(),
                record.getFromStatus() == null ? null : record.getFromStatus().name(),
                record.getToStatus() == null ? null : record.getToStatus().name(),
                record.getActorUsername(),
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
