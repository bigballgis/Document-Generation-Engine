package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Package-private visibility / role / scope guards for user management.
 */
final class UserManagementAccessSupport {

    static final String GLOBAL_ADMIN = "GLOBAL_ADMIN";
    static final String GROUP_ADMIN = "GROUP_ADMIN";

    private static final Set<ManagementRole> PRIVILEGED_ROLES = Set.of(
            ManagementRole.GLOBAL_ADMIN,
            ManagementRole.AUDIT_ADMIN,
            ManagementRole.GROUP_ADMIN
    );

    private final ManagementUserRepository managementUserRepository;
    private final ManagementAuditRecorder auditRecorder;

    UserManagementAccessSupport(
            ManagementUserRepository managementUserRepository,
            ManagementAuditRecorder auditRecorder
    ) {
        this.managementUserRepository = managementUserRepository;
        this.auditRecorder = auditRecorder;
    }

    void requireUserAdministrator(ManagementSessionClaims session) {
        if (!session.roles().contains(GLOBAL_ADMIN) && !session.roles().contains(GROUP_ADMIN)) {
            throw new UserManagementNotAllowedException();
        }
    }

    void guardRoleAssignment(ManagementSessionClaims session, Set<ManagementRole> roles) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return;
        }
        boolean assignsPrivileged = roles.stream().anyMatch(PRIVILEGED_ROLES::contains);
        if (assignsPrivileged) {
            auditRecorder.recordEscalationDenied(
                    "ROLE_ASSIGNMENT_NOT_ALLOWED",
                    session.username(),
                    UserManagementViewSupport.actorSummary(session),
                    UserManagementViewSupport.actorScopeSummary(session) + " attempted privileged role assignment"
            );
            throw new RoleAssignmentNotAllowedException();
        }
    }

    void guardScopeSubset(ManagementSessionClaims session, Set<String> requestedGroupCodes) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return;
        }
        Set<String> adminScope = new LinkedHashSet<>(session.authorizedGroupCodes());
        boolean outOfRange = !adminScope.containsAll(requestedGroupCodes);
        if (outOfRange) {
            auditRecorder.recordEscalationDenied(
                    "GROUP_SCOPE_OUT_OF_RANGE",
                    session.username(),
                    UserManagementViewSupport.actorSummary(session),
                    UserManagementViewSupport.actorScopeSummary(session) + " attempted out-of-range scope assignment"
            );
            throw new GroupScopeOutOfRangeException();
        }
    }

    ManagementUserEntity loadVisible(UUID id, ManagementSessionClaims session) {
        return managementUserRepository.findByIdAndDeletedAtIsNull(id)
                .filter(user -> isVisibleTo(session, user))
                .orElseThrow(UserNotFoundException::new);
    }

    boolean isVisibleTo(ManagementSessionClaims session, ManagementUserEntity user) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return true;
        }
        if (session.roles().contains(GROUP_ADMIN)) {
            return user.getAuthorizedGroupCodes().stream()
                    .anyMatch(code -> session.authorizedGroupCodes().contains(code));
        }
        return false;
    }

    boolean matchesGroupFilter(ManagementUserEntity user, String groupFilter) {
        return groupFilter == null || groupFilter.isBlank()
                || user.getAuthorizedGroupCodes().contains(groupFilter);
    }

    boolean matchesRoleFilter(ManagementUserEntity user, String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return true;
        }
        return user.getRoles().stream().map(Enum::name).anyMatch(roleFilter::equals);
    }
}
