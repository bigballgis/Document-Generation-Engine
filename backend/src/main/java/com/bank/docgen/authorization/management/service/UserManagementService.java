package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CreateUserRequest;
import com.bank.docgen.authorization.management.api.ManagementUserView;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.api.ResetPasswordRequest;
import com.bank.docgen.authorization.management.api.UpdateUserRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final String GLOBAL_ADMIN = "GLOBAL_ADMIN";
    private static final String GROUP_ADMIN = "GROUP_ADMIN";

    private static final Set<ManagementRole> PRIVILEGED_ROLES = Set.of(
            ManagementRole.GLOBAL_ADMIN,
            ManagementRole.AUDIT_ADMIN,
            ManagementRole.GROUP_ADMIN
    );

    private final ManagementUserRepository managementUserRepository;
    private final PasswordHashService passwordHashService;
    private final ManagementAuditRecorder auditRecorder;

    public UserManagementService(
            ManagementUserRepository managementUserRepository,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder auditRecorder
    ) {
        this.managementUserRepository = managementUserRepository;
        this.passwordHashService = passwordHashService;
        this.auditRecorder = auditRecorder;
    }

    @Transactional(readOnly = true)
    public PageView<ManagementUserView> list(
            ManagementSessionClaims session,
            String groupFilter,
            String roleFilter,
            int page,
            int size
    ) {
        List<ManagementUserView> visible = managementUserRepository.findByDeletedAtIsNullOrderByUsernameAsc().stream()
                .filter(user -> isVisibleTo(session, user))
                .filter(user -> matchesGroupFilter(user, groupFilter))
                .filter(user -> matchesRoleFilter(user, roleFilter))
                .map(UserManagementService::toView)
                .toList();
        return PageView.of(visible, page, size);
    }

    @Transactional(readOnly = true)
    public ManagementUserView get(UUID id, ManagementSessionClaims session) {
        return toView(loadVisible(id, session));
    }

    @Transactional
    public ManagementUserView create(CreateUserRequest request, ManagementSessionClaims session) {
        requireUserAdministrator(session);
        Set<ManagementRole> roles = new LinkedHashSet<>(request.roles());
        Set<String> groupCodes = new LinkedHashSet<>(request.authorizedGroupCodes());
        guardRoleAssignment(session, roles);
        guardScopeSubset(session, groupCodes);
        if (managementUserRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException();
        }
        ManagementUserEntity user = new ManagementUserEntity(
                UUID.randomUUID(),
                request.username(),
                request.displayName(),
                request.email(),
                passwordHashService.hash(request.initialPassword()),
                AuthSource.LOCAL,
                roles,
                groupCodes
        );
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_CREATED, session, user, "Created user");
        return toView(user);
    }

    @Transactional
    public ManagementUserView update(UUID id, UpdateUserRequest request, ManagementSessionClaims session) {
        ManagementUserEntity user = loadVisible(id, session);
        Set<ManagementRole> roles = new LinkedHashSet<>(request.roles());
        Set<String> groupCodes = new LinkedHashSet<>(request.authorizedGroupCodes());
        guardRoleAssignment(session, roles);
        guardScopeSubset(session, groupCodes);
        user.updateProfile(request.displayName(), request.email());
        user.assignRoles(roles);
        user.assignGroupScope(groupCodes);
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_UPDATED, session, user, "Updated user roles/scope/profile");
        return toView(user);
    }

    @Transactional
    public ManagementUserView disable(UUID id, ManagementSessionClaims session) {
        ManagementUserEntity user = loadVisible(id, session);
        user.disable();
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_DISABLED, session, user, "Disabled user");
        return toView(user);
    }

    @Transactional
    public ManagementUserView enable(UUID id, ManagementSessionClaims session) {
        ManagementUserEntity user = loadVisible(id, session);
        user.enable();
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_ENABLED, session, user, "Enabled user");
        return toView(user);
    }

    @Transactional
    public ManagementUserView resetPassword(UUID id, ResetPasswordRequest request, ManagementSessionClaims session) {
        ManagementUserEntity user = loadVisible(id, session);
        user.resetPassword(passwordHashService.hash(request.newPassword()));
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_PASSWORD_RESET, session, user, "Reset password");
        return toView(user);
    }

    @Transactional
    public ManagementUserView delete(UUID id, ManagementSessionClaims session) {
        if (!session.roles().contains(GLOBAL_ADMIN)) {
            auditRecorder.recordEscalationDenied(
                    "USER_DELETE_NOT_ALLOWED",
                    session.username(),
                    actorSummary(session),
                    actorScopeSummary(session) + " attempted user delete"
            );
            throw new UserDeleteNotAllowedException();
        }
        ManagementUserEntity user = managementUserRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(UserNotFoundException::new);
        user.markDeleted();
        managementUserRepository.save(user);
        recordUserEvent(ManagementAuditRecorder.USER_DELETED, session, user, "Logically deleted user");
        return toView(user);
    }

    private void requireUserAdministrator(ManagementSessionClaims session) {
        if (!session.roles().contains(GLOBAL_ADMIN) && !session.roles().contains(GROUP_ADMIN)) {
            throw new UserManagementNotAllowedException();
        }
    }

    private void guardRoleAssignment(ManagementSessionClaims session, Set<ManagementRole> roles) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return;
        }
        boolean assignsPrivileged = roles.stream().anyMatch(PRIVILEGED_ROLES::contains);
        if (assignsPrivileged) {
            auditRecorder.recordEscalationDenied(
                    "ROLE_ASSIGNMENT_NOT_ALLOWED",
                    session.username(),
                    actorSummary(session),
                    actorScopeSummary(session) + " attempted privileged role assignment"
            );
            throw new RoleAssignmentNotAllowedException();
        }
    }

    private void guardScopeSubset(ManagementSessionClaims session, Set<String> requestedGroupCodes) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return;
        }
        Set<String> adminScope = new LinkedHashSet<>(session.authorizedGroupCodes());
        boolean outOfRange = !adminScope.containsAll(requestedGroupCodes);
        if (outOfRange) {
            auditRecorder.recordEscalationDenied(
                    "GROUP_SCOPE_OUT_OF_RANGE",
                    session.username(),
                    actorSummary(session),
                    actorScopeSummary(session) + " attempted out-of-range scope assignment"
            );
            throw new GroupScopeOutOfRangeException();
        }
    }

    private ManagementUserEntity loadVisible(UUID id, ManagementSessionClaims session) {
        return managementUserRepository.findByIdAndDeletedAtIsNull(id)
                .filter(user -> isVisibleTo(session, user))
                .orElseThrow(UserNotFoundException::new);
    }

    private boolean isVisibleTo(ManagementSessionClaims session, ManagementUserEntity user) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return true;
        }
        if (session.roles().contains(GROUP_ADMIN)) {
            return user.getAuthorizedGroupCodes().stream()
                    .anyMatch(code -> session.authorizedGroupCodes().contains(code));
        }
        return false;
    }

    private boolean matchesGroupFilter(ManagementUserEntity user, String groupFilter) {
        return groupFilter == null || groupFilter.isBlank()
                || user.getAuthorizedGroupCodes().contains(groupFilter);
    }

    private boolean matchesRoleFilter(ManagementUserEntity user, String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return true;
        }
        return user.getRoles().stream().map(Enum::name).anyMatch(roleFilter::equals);
    }

    private void recordUserEvent(
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

    private String actorSummary(ManagementSessionClaims session) {
        return session.displayName() + " (" + session.username() + ")";
    }

    private String actorScopeSummary(ManagementSessionClaims session) {
        if (session.roles().contains(GLOBAL_ADMIN)) {
            return "actorAuthorizedGroupScope=*";
        }
        return "actorAuthorizedGroupScope=" + new LinkedHashSet<>(session.authorizedGroupCodes());
    }

    private static ManagementUserView toView(ManagementUserEntity user) {
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
}
