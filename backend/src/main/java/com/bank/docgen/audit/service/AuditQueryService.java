package com.bank.docgen.audit.service;

import com.bank.docgen.audit.api.GenerationAuditQueryResult;
import com.bank.docgen.audit.api.LifecycleAuditExportResult;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.api.ManagementAuditExportResult;
import com.bank.docgen.audit.api.ManagementAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService {

    public static final String EXPORT_FORMAT = "management-audit-export-v1-json";
    public static final String LIFECYCLE_EXPORT_FORMAT = "lifecycle-audit-export-v1-json";

    private final GroupAccessService groupAccessService;
    private final AuditQueryManagementSupport managementSupport;
    private final AuditQueryLifecycleSupport lifecycleSupport;

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
        this.groupAccessService = groupAccessService;
        AuditQueryAccessSupport accessSupport = new AuditQueryAccessSupport(groupAccessService, templateService);
        AuditEventViewMapper viewMapper = new AuditEventViewMapper(
                templateService,
                managementUserDisplayService,
                auditMaskingService,
                objectMapper
        );
        this.managementSupport = new AuditQueryManagementSupport(
                managementAuditEventRepository,
                runtimeGenerationAuditEventRepository,
                templateService,
                groupAccessService,
                accessSupport,
                viewMapper
        );
        this.lifecycleSupport = new AuditQueryLifecycleSupport(
                lifecycleRecordRepository,
                accessSupport,
                viewMapper
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
        return managementSupport.queryManagementEvents(
                session, actorRole, templateId, eventType, credentialId,
                eventAtFrom, eventAtTo, groupScope, requestId, page, size);
    }

    @Transactional(readOnly = true)
    public GenerationAuditQueryResult queryGenerationEventsByExternalId(
            ManagementSessionClaims session,
            String templateExternalId,
            Integer page,
            Integer size
    ) {
        return managementSupport.queryGenerationEventsByExternalId(
                session, templateExternalId, page, size);
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
        return managementSupport.exportManagementEvents(
                session, actorRole, templateId, eventType, credentialId,
                eventAtFrom, eventAtTo, groupScope, requestId);
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
        return lifecycleSupport.queryLifecycleEvents(
                session, actorRole, templateId, eventType, eventAtFrom, eventAtTo, groupScope, requestId, page, size);
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
        return lifecycleSupport.exportLifecycleEvents(
                session, actorRole, templateId, eventType, eventAtFrom, eventAtTo, groupScope, requestId);
    }
}
