package com.bank.docgen.audit.service;

import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateService;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private access / filter / time-window guards for audit queries.
 */
final class AuditQueryAccessSupport {

    private final GroupAccessService groupAccessService;
    private final TemplateService templateService;

    AuditQueryAccessSupport(GroupAccessService groupAccessService, TemplateService templateService) {
        this.groupAccessService = groupAccessService;
        this.templateService = templateService;
    }

    void requireCanReadAudit(ManagementSessionClaims session) {
        if (!groupAccessService.canReadAudit(session)) {
            throw new AuditAccessDeniedException();
        }
    }

    String resolveGroupFilter(
            ManagementSessionClaims session,
            AuditReadActorRole actorRole,
            UUID templateId,
            String groupScope
    ) {
        requireCanReadAudit(session);
        validateActorRole(session, actorRole);
        return switch (actorRole) {
            case AUDIT_ADMIN, GLOBAL_ADMIN -> null;
            case GROUP_ADMIN -> resolveGroupAdminScope(session, templateId, groupScope);
        };
    }

    void validateActorRole(ManagementSessionClaims session, AuditReadActorRole actorRole) {
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

    UUID resolveLifecycleTemplateId(
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

    void validateTimeWindow(Instant eventAtFrom, Instant eventAtTo) {
        if (eventAtFrom != null && eventAtTo != null && eventAtFrom.isAfter(eventAtTo)) {
            throw new AuditValidationException(
                    ApiErrorCodes.INVALID_TIME_WINDOW,
                    "api.error.audit.invalidTimeWindow"
            );
        }
    }

    static String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
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
}
