package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.ManagementAuditEventTypes;
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

    private final ManagementUserRepository managementUserRepository;
    private final PasswordHashService passwordHashService;
    private final ManagementAuditRecorder auditRecorder;
    private final UserManagementAccessSupport access;

    public UserManagementService(
            ManagementUserRepository managementUserRepository,
            PasswordHashService passwordHashService,
            ManagementAuditRecorder auditRecorder
    ) {
        this.managementUserRepository = managementUserRepository;
        this.passwordHashService = passwordHashService;
        this.auditRecorder = auditRecorder;
        this.access = new UserManagementAccessSupport(managementUserRepository, auditRecorder);
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
                .filter(user -> access.isVisibleTo(session, user))
                .filter(user -> access.matchesGroupFilter(user, groupFilter))
                .filter(user -> access.matchesRoleFilter(user, roleFilter))
                .map(UserManagementViewSupport::toView)
                .toList();
        return PageView.of(visible, page, size);
    }

    @Transactional(readOnly = true)
    public ManagementUserView get(UUID id, ManagementSessionClaims session) {
        return UserManagementViewSupport.toView(access.loadVisible(id, session));
    }

    @Transactional
    public ManagementUserView create(CreateUserRequest request, ManagementSessionClaims session) {
        access.requireUserAdministrator(session);
        Set<ManagementRole> roles = new LinkedHashSet<>(request.roles());
        Set<String> groupCodes = new LinkedHashSet<>(request.authorizedGroupCodes());
        access.guardRoleAssignment(session, roles);
        access.guardScopeSubset(session, groupCodes);
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
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_CREATED, session, user, "Created user");
        return UserManagementViewSupport.toView(user);
    }

    @Transactional
    public ManagementUserView update(UUID id, UpdateUserRequest request, ManagementSessionClaims session) {
        ManagementUserEntity user = access.loadVisible(id, session);
        Set<ManagementRole> roles = new LinkedHashSet<>(request.roles());
        Set<String> groupCodes = new LinkedHashSet<>(request.authorizedGroupCodes());
        access.guardRoleAssignment(session, roles);
        access.guardScopeSubset(session, groupCodes);
        user.updateProfile(request.displayName(), request.email());
        user.assignRoles(roles);
        user.assignGroupScope(groupCodes);
        managementUserRepository.save(user);
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_UPDATED, session, user, "Updated user roles/scope/profile");
        return UserManagementViewSupport.toView(user);
    }

    @Transactional
    public ManagementUserView disable(UUID id, ManagementSessionClaims session) {
        ManagementUserEntity user = access.loadVisible(id, session);
        user.disable();
        managementUserRepository.save(user);
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_DISABLED, session, user, "Disabled user");
        return UserManagementViewSupport.toView(user);
    }

    @Transactional
    public ManagementUserView enable(UUID id, ManagementSessionClaims session) {
        ManagementUserEntity user = access.loadVisible(id, session);
        user.enable();
        managementUserRepository.save(user);
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_ENABLED, session, user, "Enabled user");
        return UserManagementViewSupport.toView(user);
    }

    @Transactional
    public ManagementUserView resetPassword(UUID id, ResetPasswordRequest request, ManagementSessionClaims session) {
        ManagementUserEntity user = access.loadVisible(id, session);
        user.resetPassword(passwordHashService.hash(request.newPassword()));
        managementUserRepository.save(user);
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_PASSWORD_RESET, session, user, "Reset password");
        return UserManagementViewSupport.toView(user);
    }

    @Transactional
    public ManagementUserView delete(UUID id, ManagementSessionClaims session) {
        if (!session.roles().contains(UserManagementAccessSupport.GLOBAL_ADMIN)) {
            auditRecorder.recordEscalationDenied(
                    "USER_DELETE_NOT_ALLOWED",
                    session.username(),
                    UserManagementViewSupport.actorSummary(session),
                    UserManagementViewSupport.actorScopeSummary(session) + " attempted user delete"
            );
            throw new UserDeleteNotAllowedException();
        }
        ManagementUserEntity user = managementUserRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(UserNotFoundException::new);
        user.markDeleted();
        managementUserRepository.save(user);
        UserManagementViewSupport.recordUserEvent(
                auditRecorder, ManagementAuditEventTypes.USER_DELETED, session, user, "Logically deleted user");
        return UserManagementViewSupport.toView(user);
    }
}
