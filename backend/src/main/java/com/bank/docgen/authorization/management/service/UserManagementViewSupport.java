package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.ManagementUserView;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.LinkedHashSet;

/**
 * Package-private view mapping and actor summary helpers for user management.
 */
final class UserManagementViewSupport {

    private UserManagementViewSupport() {
    }

    static ManagementUserView toView(ManagementUserEntity user) {
        return new ManagementUserView(
                user.getId().toString(),
                user.getUsername(),
                user.getDisplayName(),
                user.getEmail(),
                user.getAuthSource().name(),
                user.getRoles().stream().map(Enum::name).sorted().toList(),
                user.getAuthorizedGroupCodes().stream().sorted().toList(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    static String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    static String actorScopeSummary(ManagementSessionClaims session) {
        if (session.roles().contains(UserManagementAccessSupport.GLOBAL_ADMIN)) {
            return "actorAuthorizedGroupScope=*";
        }
        return "actorAuthorizedGroupScope=" + new LinkedHashSet<>(session.authorizedGroupCodes());
    }

    static void recordUserEvent(
            ManagementAuditRecorder auditRecorder,
            String eventType,
            ManagementSessionClaims session,
            ManagementUserEntity user,
            String action
    ) {
        auditRecorder.recordUserEvent(
                eventType,
                session.username(),
                actorSummary(session),
                action + " " + user.getUsername() + "; " + actorScopeSummary(session)
        );
    }
}
