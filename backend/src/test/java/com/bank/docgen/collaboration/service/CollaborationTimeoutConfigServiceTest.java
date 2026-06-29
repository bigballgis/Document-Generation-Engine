package com.bank.docgen.collaboration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.api.UpsertCollaborationTimeoutConfigRequest;
import com.bank.docgen.collaboration.domain.CollaborationTimeoutScope;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigEntity;
import com.bank.docgen.collaboration.persistence.CollaborationTimeoutConfigRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollaborationTimeoutConfigServiceTest {

    @Mock
    private CollaborationTimeoutConfigRepository repository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private CollaborationTimeoutConfigService service;
    private CollaborationTimeoutResolver resolver;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAdmin;
    private ManagementSessionClaims tester;

    @BeforeEach
    void setUp() {
        resolver = new CollaborationTimeoutResolver(repository);
        service = new CollaborationTimeoutConfigService(repository, resolver, groupAccessService, auditRecorder);
        globalAdmin = session("10000001", List.of("GLOBAL_ADMIN"), List.of("*"));
        groupAdmin = session("10000002", List.of("GROUP_ADMIN"), List.of("RETAIL"));
        tester = session("10000003", List.of("TEMPLATE_TESTER"), List.of("RETAIL"));
    }

    @Test
    void resolveGroup_fallsBackToGlobalWhenNoOverride() {
        CollaborationTimeoutConfigEntity global = entity(CollaborationTimeoutScope.GLOBAL, null, 72, 72, 48, 168);
        when(groupAccessService.canMaintainCollaborationTimeoutConfig(groupAdmin)).thenReturn(true);
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GROUP, "RETAIL"))
                .thenReturn(Optional.empty());
        when(repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GLOBAL, null))
                .thenReturn(Optional.of(global));

        CollaborationTimeoutConfigView view = service.resolve("RETAIL", groupAdmin);

        assertThat(view.scopeType()).isEqualTo("GLOBAL");
        assertThat(view.testThresholdHours()).isEqualTo(72);
        assertThat(view.remediationThresholdHours()).isEqualTo(168);
    }

    @Test
    void resolveGroup_returnsGroupOverrideWhenPresent() {
        CollaborationTimeoutConfigEntity group = entity(CollaborationTimeoutScope.GROUP, "RETAIL", 24, 24, 12, 96);
        when(groupAccessService.canMaintainCollaborationTimeoutConfig(groupAdmin)).thenReturn(true);
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GROUP, "RETAIL"))
                .thenReturn(Optional.of(group));

        CollaborationTimeoutConfigView view = service.resolve("RETAIL", groupAdmin);

        assertThat(view.scopeType()).isEqualTo("GROUP");
        assertThat(view.groupCode()).isEqualTo("RETAIL");
        assertThat(view.testThresholdHours()).isEqualTo(24);
    }

    @Test
    void resolveGlobal_requiresGlobalAdmin() {
        assertThatThrownBy(() -> service.resolve(null, groupAdmin))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void resolve_deniesNonAdminRoles() {
        assertThatThrownBy(() -> service.resolve("RETAIL", tester))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    @Test
    void upsertGroupOverride_isAudited() {
        when(groupAccessService.canMaintainCollaborationTimeoutConfig(groupAdmin)).thenReturn(true);
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(repository.findByScopeTypeAndGroupCode(CollaborationTimeoutScope.GROUP, "RETAIL"))
                .thenReturn(Optional.empty());
        when(repository.save(any(CollaborationTimeoutConfigEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertCollaborationTimeoutConfigRequest request = new UpsertCollaborationTimeoutConfigRequest(
                "GROUP",
                "RETAIL",
                24,
                24,
                12,
                96
        );

        service.upsert(request, groupAdmin);

        verify(auditRecorder).recordCollaborationTimeoutConfigUpdated(
                eq("GROUP"),
                eq("RETAIL"),
                eq("10000002"),
                eq("10000002"),
                any(String.class)
        );
    }

    @Test
    void upsertGlobal_requiresGlobalAdmin() {
        UpsertCollaborationTimeoutConfigRequest request = new UpsertCollaborationTimeoutConfigRequest(
                "GLOBAL",
                null,
                72,
                72,
                48,
                168
        );

        assertThatThrownBy(() -> service.upsert(request, groupAdmin))
                .isInstanceOf(CollaborationWorkItemAccessDeniedException.class);
    }

    private CollaborationTimeoutConfigEntity entity(
            CollaborationTimeoutScope scope,
            String groupCode,
            int testHours,
            int approvalHours,
            int pendingReleaseHours,
            int remediationHours
    ) {
        return new CollaborationTimeoutConfigEntity(
                UUID.randomUUID(),
                scope,
                groupCode,
                testHours,
                approvalHours,
                pendingReleaseHours,
                remediationHours
        );
    }

    private ManagementSessionClaims session(String username, List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
