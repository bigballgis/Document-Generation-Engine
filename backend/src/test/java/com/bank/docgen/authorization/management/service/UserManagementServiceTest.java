package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CreateUserRequest;
import com.bank.docgen.authorization.management.api.ManagementUserView;
import com.bank.docgen.authorization.management.api.ResetPasswordRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.ManagementRole;
import com.bank.docgen.authorization.management.persistence.ManagementUserEntity;
import com.bank.docgen.authorization.management.persistence.ManagementUserRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private ManagementUserRepository userRepository;

    @Mock
    private PasswordHashService passwordHashService;

    @Mock
    private ManagementAuditRecorder auditRecorder;

    @InjectMocks
    private UserManagementService service;

    @Test
    void globalAdminCreatesUser() {
        when(userRepository.existsByUsername("20000001")).thenReturn(false);
        when(passwordHashService.hash(anyString())).thenReturn("HASH");

        ManagementUserView view = service.create(
                createRequest("20000001", List.of(ManagementRole.TEMPLATE_AUTHOR), List.of("RETAIL")),
                globalAdmin());

        assertThat(view.username()).isEqualTo("20000001");
        assertThat(view.authorizedGroupCodes()).containsExactly("RETAIL");
        verify(userRepository).save(any(ManagementUserEntity.class));
        verify(auditRecorder).recordUserEvent(
                org.mockito.ArgumentMatchers.eq(ManagementAuditRecorder.USER_CREATED),
                anyString(), anyString(), anyString());
    }

    @Test
    void duplicateUsernameReturnsConflict() {
        when(userRepository.existsByUsername("20000001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                createRequest("20000001", List.of(ManagementRole.TEMPLATE_AUTHOR), List.of("RETAIL")),
                globalAdmin()))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void nonAdminCannotCreateUser() {
        ManagementSessionClaims author = session(List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"));

        assertThatThrownBy(() -> service.create(
                createRequest("20000002", List.of(ManagementRole.TEMPLATE_AUTHOR), List.of("RETAIL")),
                author))
                .isInstanceOf(UserManagementNotAllowedException.class);
    }

    @Test
    void groupAdminCannotAssignPrivilegedRole() {
        assertThatThrownBy(() -> service.create(
                createRequest("20000003", List.of(ManagementRole.GLOBAL_ADMIN), List.of("RETAIL")),
                groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(RoleAssignmentNotAllowedException.class);
        verify(auditRecorder).recordEscalationDenied(
                org.mockito.ArgumentMatchers.eq("ROLE_ASSIGNMENT_NOT_ALLOWED"),
                anyString(), anyString(), anyString());
    }

    @Test
    void groupAdminCannotAssignOutOfRangeScope() {
        assertThatThrownBy(() -> service.create(
                createRequest("20000004", List.of(ManagementRole.TEMPLATE_AUTHOR), List.of("RETAIL", "CORP")),
                groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(GroupScopeOutOfRangeException.class);
        verify(auditRecorder).recordEscalationDenied(
                org.mockito.ArgumentMatchers.eq("GROUP_SCOPE_OUT_OF_RANGE"),
                anyString(), anyString(), anyString());
    }

    @Test
    void groupAdminCreatesWithinScope() {
        when(userRepository.existsByUsername("20000005")).thenReturn(false);
        when(passwordHashService.hash(anyString())).thenReturn("HASH");

        ManagementUserView view = service.create(
                createRequest("20000005", List.of(ManagementRole.TEMPLATE_TESTER), List.of("RETAIL")),
                groupAdmin(List.of("RETAIL", "CORP")));

        assertThat(view.roles()).containsExactly("TEMPLATE_TESTER");
    }

    @Test
    void groupAdminCannotDeleteUser() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID(), groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(UserDeleteNotAllowedException.class);
        verify(auditRecorder).recordEscalationDenied(
                org.mockito.ArgumentMatchers.eq("USER_DELETE_NOT_ALLOWED"),
                anyString(), anyString(), anyString());
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void globalAdminDeletesUser() {
        ManagementUserEntity user = user("20000006", Set.of(ManagementRole.TEMPLATE_AUTHOR), Set.of("RETAIL"));
        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        service.delete(user.getId(), globalAdmin());

        assertThat(user.isDeleted()).isTrue();
        verify(auditRecorder).recordUserEvent(
                org.mockito.ArgumentMatchers.eq(ManagementAuditRecorder.USER_DELETED),
                anyString(), anyString(), anyString());
    }

    @Test
    void groupAdminCannotSeeOutOfScopeUser() {
        ManagementUserEntity corpUser = user("20000007", Set.of(ManagementRole.TEMPLATE_AUTHOR), Set.of("CORP"));
        when(userRepository.findByIdAndDeletedAtIsNull(corpUser.getId())).thenReturn(Optional.of(corpUser));

        assertThatThrownBy(() -> service.get(corpUser.getId(), groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void globalAdminResetsPasswordWithoutLeakingHash() {
        ManagementUserEntity user = user("20000008", Set.of(ManagementRole.TEMPLATE_AUTHOR), Set.of("RETAIL"));
        when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));
        when(passwordHashService.hash("NewSecret1234")).thenReturn("NEWHASH");

        service.resetPassword(user.getId(), new ResetPasswordRequest("NewSecret1234"), globalAdmin());

        assertThat(user.getPasswordHash()).isEqualTo("NEWHASH");
        verify(auditRecorder).recordUserEvent(
                org.mockito.ArgumentMatchers.eq(ManagementAuditRecorder.USER_PASSWORD_RESET),
                anyString(), anyString(), anyString());
    }

    @Test
    void globalAdminDisablesAndEnablesUser() {
        ManagementUserEntity user = user("20000009", Set.of(ManagementRole.TEMPLATE_AUTHOR), Set.of("RETAIL"));
        lenient().when(userRepository.findByIdAndDeletedAtIsNull(user.getId())).thenReturn(Optional.of(user));

        assertThat(service.disable(user.getId(), globalAdmin()).enabled()).isFalse();
        assertThat(service.enable(user.getId(), globalAdmin()).enabled()).isTrue();
    }

    @Test
    void listAppliesScopeAndFilters() {
        when(userRepository.findByDeletedAtIsNullOrderByUsernameAsc()).thenReturn(List.of(
                user("20000010", Set.of(ManagementRole.TEMPLATE_AUTHOR), Set.of("RETAIL")),
                user("20000011", Set.of(ManagementRole.TEMPLATE_TESTER), Set.of("CORP"))
        ));

        var page = service.list(groupAdmin(List.of("RETAIL")), null, null, 0, 20);

        assertThat(page.content()).extracting(ManagementUserView::username).containsExactly("20000010");
    }

    private static CreateUserRequest createRequest(String username, List<ManagementRole> roles, List<String> groups) {
        return new CreateUserRequest(username, "Name", username + "@example.com", "InitSecret1234", roles, groups);
    }

    private static ManagementUserEntity user(String username, Set<ManagementRole> roles, Set<String> groups) {
        return new ManagementUserEntity(
                UUID.randomUUID(), username, "Name", username + "@example.com",
                "HASH", AuthSource.LOCAL, roles, groups);
    }

    private static ManagementSessionClaims globalAdmin() {
        return session(List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    private static ManagementSessionClaims groupAdmin(List<String> groups) {
        return session(List.of("GROUP_ADMIN"), groups);
    }

    private static ManagementSessionClaims session(List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                "10000001", "Admin", "admin@example.com", AuthSource.LOCAL,
                roles, groups, "route.global-governance-home", List.of(),
                Instant.now().plusSeconds(3600));
    }
}
