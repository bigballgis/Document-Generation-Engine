package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.BusinessGroupView;
import com.bank.docgen.authorization.management.api.CreateGroupRequest;
import com.bank.docgen.authorization.management.api.UpdateGroupRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.domain.GroupDimension;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BusinessGroupServiceTest {

    @Mock
    private BusinessGroupRepository businessGroupRepository;

    @Mock
    private ManagementAuditRecorder auditRecorder;

    @InjectMocks
    private BusinessGroupService service;

    @Test
    void globalAdminCreatesGroup() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("WEALTH")).thenReturn(false);

        BusinessGroupView view = service.create(
                new CreateGroupRequest("WEALTH", "Wealth Management", GroupDimension.DEPARTMENT),
                globalAdmin()
        );

        assertThat(view.groupCode()).isEqualTo("WEALTH");
        assertThat(view.dimension()).isEqualTo("DEPARTMENT");
        assertThat(view.enabled()).isTrue();
        verify(businessGroupRepository).save(any(BusinessGroupEntity.class));
        verify(auditRecorder).recordGroupEvent(
                org.mockito.ArgumentMatchers.eq(ManagementAuditRecorder.GROUP_CREATED),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void groupAdminCannotCreateGroup() {
        assertThatThrownBy(() -> service.create(
                new CreateGroupRequest("WEALTH", "Wealth", GroupDimension.DEPARTMENT),
                groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(GroupManagementNotAllowedException.class);
        verify(businessGroupRepository, never()).save(any());
        verify(auditRecorder).recordEscalationDenied(
                org.mockito.ArgumentMatchers.eq("GROUP_MANAGEMENT_NOT_ALLOWED"),
                anyString(), anyString(), anyString());
    }

    @Test
    void duplicateGroupCodeReturnsConflict() {
        when(businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull("RETAIL")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateGroupRequest("RETAIL", "Retail", GroupDimension.BUSINESS_LINE),
                globalAdmin()))
                .isInstanceOf(GroupCodeAlreadyExistsException.class);
    }

    @Test
    void missingGroupReturnsNotFound() {
        UUID id = UUID.randomUUID();
        when(businessGroupRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id, globalAdmin()))
                .isInstanceOf(GroupNotFoundException.class);
    }

    @Test
    void groupAdminCannotSeeOutOfScopeGroup() {
        BusinessGroupEntity corp = group("CORP", "Corporate");
        when(businessGroupRepository.findByIdAndDeletedAtIsNull(corp.getId())).thenReturn(Optional.of(corp));

        assertThatThrownBy(() -> service.get(corp.getId(), groupAdmin(List.of("RETAIL"))))
                .isInstanceOf(GroupNotFoundException.class);
    }

    @Test
    void globalAdminUpdatesDisplayName() {
        BusinessGroupEntity retail = group("RETAIL", "Retail");
        when(businessGroupRepository.findByIdAndDeletedAtIsNull(retail.getId())).thenReturn(Optional.of(retail));

        BusinessGroupView view = service.updateDisplayName(
                retail.getId(), new UpdateGroupRequest("Retail Renamed"), globalAdmin());

        assertThat(view.displayName()).isEqualTo("Retail Renamed");
        verify(auditRecorder).recordGroupEvent(
                org.mockito.ArgumentMatchers.eq(ManagementAuditRecorder.GROUP_UPDATED),
                anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void globalAdminDisablesAndEnablesGroup() {
        BusinessGroupEntity retail = group("RETAIL", "Retail");
        when(businessGroupRepository.findByIdAndDeletedAtIsNull(retail.getId())).thenReturn(Optional.of(retail));

        assertThat(service.disable(retail.getId(), globalAdmin()).enabled()).isFalse();
        assertThat(service.enable(retail.getId(), globalAdmin()).enabled()).isTrue();
    }

    @Test
    void groupAdminListSeesOnlyScopedGroups() {
        when(businessGroupRepository.findByDeletedAtIsNullOrderByGroupCodeAsc())
                .thenReturn(List.of(group("RETAIL", "Retail"), group("CORP", "Corporate")));

        var page = service.list(groupAdmin(List.of("RETAIL")), 0, 20);

        assertThat(page.content()).extracting(BusinessGroupView::groupCode).containsExactly("RETAIL");
    }

    private static BusinessGroupEntity group(String code, String name) {
        return new BusinessGroupEntity(UUID.randomUUID(), code, name, GroupDimension.BUSINESS_LINE);
    }

    private static ManagementSessionClaims globalAdmin() {
        return session(List.of("GLOBAL_ADMIN"), List.of("*"));
    }

    private static ManagementSessionClaims groupAdmin(List<String> groups) {
        return session(List.of("GROUP_ADMIN"), groups);
    }

    private static ManagementSessionClaims session(List<String> roles, List<String> groups) {
        return new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.global-governance-home",
                List.of(),
                Instant.now().plusSeconds(3600)
        );
    }
}
